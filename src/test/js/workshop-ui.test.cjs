const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const source = fs.readFileSync(
  path.resolve(__dirname, "../../main/webapp/workshop-ui/workshop.js"),
  "utf8"
);

function createFormData(form) {
  return {
    entries() {
      return Object.entries(form.values || {});
    }
  };
}

async function renderPage({ pathname, search = "", responses, postResponse = { updated: true } }) {
  const listeners = {};
  const fetchCalls = [];
  const root = {
    dataset: { contextPath: "" },
    innerHTML: "",
    addEventListener(type, callback) {
      listeners[type] = callback;
    }
  };
  const toast = { innerHTML: "", isConnected: true };
  const document = {
    getElementById(id) {
      if (id === "workshop-app") return root;
      if (id === "workshop-toast") return toast;
      return null;
    },
    querySelector(selector) {
      if (selector === 'meta[name="csrf-token"]') return { content: "csrf-test-token" };
      return null;
    }
  };
  const window = {
    location: {
      pathname,
      search,
      href: `http://localhost${pathname}${search}`,
      assign() {}
    },
    setTimeout() {
      return 1;
    }
  };
  const fetch = async (input, options = {}) => {
    const url = new URL(String(input), "http://localhost");
    const method = String(options.method || "GET").toUpperCase();
    fetchCalls.push({ url, method, body: options.body || null, headers: options.headers || {} });

    if (method === "POST") {
      return {
        ok: true,
        status: 200,
        async json() { return { data: postResponse }; }
      };
    }

    const value = responses[url.pathname];
    if (value === undefined) {
      return {
        ok: false,
        status: 404,
        async json() { return { error: `Unexpected API path: ${url.pathname}` }; }
      };
    }
    if (value && value.httpStatus) {
      return {
        ok: value.httpStatus >= 200 && value.httpStatus < 300,
        status: value.httpStatus,
        async json() { return value.payload; }
      };
    }
    return {
      ok: true,
      status: 200,
      async json() { return { data: value }; }
    };
  };

  const context = {
    document,
    window,
    fetch,
    URL,
    URLSearchParams,
    FormData: createFormData,
    Intl,
    Date,
    console
  };
  vm.runInNewContext(source, context, { filename: "workshop.js" });

  for (let attempt = 0; attempt < 10; attempt += 1) {
    await new Promise(resolve => setImmediate(resolve));
    if (!root.innerHTML.includes("Đang tải dữ liệu xưởng")) break;
  }

  return { root, listeners, fetchCalls, toast };
}

const shopSession = {
  name: "Chủ xưởng",
  email: "owner@example.test",
  shops: [{ id: 11, name: "Xưởng thử nghiệm" }]
};

test("message page renders an owned order thread and escapes message content", async () => {
  const { root, fetchCalls } = await renderPage({
    pathname: "/workshop/messages",
    search: "?shopId=11&orderId=42",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/messages": [{
        orderId: 42,
        orderCode: "PK-00042",
        customerName: "Khách thử nghiệm",
        lastMessage: "Xin chào",
        unread: 2,
        lastMessageAt: ""
      }],
      "/workshop/api/messages/42": [{
        id: 1,
        sender: "Khách thử nghiệm",
        body: "<img src=x onerror=alert(1)>",
        createdAt: "",
        mine: false
      }]
    }
  });

  assert.match(root.innerHTML, /Tin nhắn theo đơn hàng/);
  assert.match(root.innerHTML, /PK-00042/);
  assert.match(root.innerHTML, /&lt;img src=x onerror=alert\(1\)&gt;/);
  assert.doesNotMatch(root.innerHTML, /<img src=x/);
  assert.doesNotMatch(root.innerHTML, /2 mới/);
  assert.ok(fetchCalls.some(call => call.url.pathname === "/workshop/api/messages/42"));
});

test("change-request page renders request information and response actions", async () => {
  const { root, fetchCalls } = await renderPage({
    pathname: "/workshop/change-requests",
    search: "?shopId=11&status=ALL",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/change-requests": [{
        id: 7,
        orderId: 42,
        orderCode: "PK-00042",
        customer: "Khách thử nghiệm",
        type: "CHANGE_MATERIAL",
        reason: "Muốn đổi sang PLA",
        payload: JSON.stringify({ proposedValue: "PLA" }),
        status: "PENDING",
        createdAt: "",
        itemId: 3
      }]
    }
  });

  assert.match(root.innerHTML, /Yêu cầu thay đổi/);
  assert.match(root.innerHTML, /Đổi vật liệu/);
  assert.match(root.innerHTML, /Muốn đổi sang PLA/);
  assert.match(root.innerHTML, /value="ACCEPTED"/);
  assert.match(root.innerHTML, /value="REJECTED"/);
  const listCall = fetchCalls.find(call => call.url.pathname === "/workshop/api/change-requests");
  assert.ok(listCall);
  assert.equal(listCall.url.searchParams.has("status"), false);
});

test("message form posts to the selected order endpoint with the session CSRF header", async () => {
  const { listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/messages",
    search: "?shopId=11&orderId=42",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/messages": [{
        orderId: 42, orderCode: "PK-00042", customerName: "Khách thử nghiệm",
        lastMessage: "", unread: 0, lastMessageAt: ""
      }],
      "/workshop/api/messages/42": []
    }
  });

  const form = {
    dataset: { form: "message", orderId: "42" },
    values: { message: "Tôi đã nhận được yêu cầu." }
  };
  let prevented = false;
  await listeners.submit({
    target: { closest: () => form },
    submitter: null,
    preventDefault() { prevented = true; }
  });

  const post = fetchCalls.find(call => call.method === "POST");
  assert.equal(prevented, true);
  assert.ok(post);
  assert.equal(post.url.pathname, "/workshop/api/messages/42");
  assert.equal(new URLSearchParams(post.body).get("message"), "Tôi đã nhận được yêu cầu.");
  assert.equal(post.headers["X-CSRF-Token"], "csrf-test-token");
});

test("change-request form posts the selected decision and adjustment values", async () => {
  const { listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/change-requests",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/change-requests": [{
        id: 7, orderId: 42, orderCode: "PK-00042", customer: "Khách thử nghiệm",
        type: "CHANGE_COLOR", reason: "Đổi màu", payload: "{}", status: "PENDING",
        createdAt: "", itemId: 3
      }]
    }
  });

  const form = {
    dataset: { form: "change-request", id: "7" },
    values: { priceDelta: "15000", cancelFee: "0", note: "Xưởng đồng ý đổi màu." }
  };
  let prevented = false;
  await listeners.submit({
    target: { closest: () => form },
    submitter: { value: "ACCEPTED" },
    preventDefault() { prevented = true; }
  });

  const post = fetchCalls.find(call => call.method === "POST");
  assert.equal(prevented, true);
  assert.ok(post);
  assert.equal(post.url.pathname, "/workshop/api/change-requests/7/respond");
  const body = new URLSearchParams(post.body);
  assert.equal(body.get("action"), "ACCEPTED");
  assert.equal(body.get("priceDelta"), "15000");
  assert.equal(body.get("cancelFee"), "0");
  assert.equal(body.get("note"), "Xưởng đồng ý đổi màu.");
  assert.equal(post.headers["X-CSRF-Token"], "csrf-test-token");
});


test("message page shows the CSRF error supplied by the access filter", async () => {
  const { root } = await renderPage({
    pathname: "/workshop/messages",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/messages": {
        httpStatus: 403,
        payload: {
          success: false,
          message: "Phiên làm việc đã hết hạn. Vui lòng tải lại trang và thử lại."
        }
      }
    }
  });

  assert.match(root.innerHTML, /Phiên làm việc đã hết hạn/);
  assert.doesNotMatch(root.innerHTML, /Yêu cầu chưa hoàn tất/);
});


const existingPageScenarios = [
  {
    name: "dashboard",
    pathname: "/workshop/dashboard",
    expectedHeading: "Tổng quan xưởng",
    endpoint: "/workshop/api/dashboard",
    response: {
      shop: { verificationStatus: "VERIFIED", status: "ACTIVE", rating: 4.8 },
      metrics: { newQuotes: 0, activeOrders: 0, completedOrders: 0, cancelledOrders: 0, offers: 0, publishedProducts: 0, pendingReviewProducts: 0 },
      recentOrders: [],
      recentQuotes: []
    }
  },
  {
    name: "profile",
    pathname: "/workshop/profile",
    expectedHeading: "Hồ sơ & năng lực",
    endpoint: "/workshop/api/profile",
    response: {}
  },
  {
    name: "products",
    pathname: "/workshop/products",
    expectedHeading: "Sản phẩm & bảng giá",
    endpoint: "/workshop/api/offers",
    response: { items: [], options: [[]], page: 1, pages: 1, total: 0 }
  },
  {
    name: "orders",
    pathname: "/workshop/orders",
    expectedHeading: "Đơn hàng & tiến độ",
    endpoint: "/workshop/api/orders",
    response: { items: [], options: [[]], page: 1, pages: 1, total: 0 }
  },
  {
    name: "quotes",
    pathname: "/workshop/quotes",
    expectedHeading: "Yêu cầu báo giá",
    endpoint: "/workshop/api/quotes",
    response: { items: [], options: [[]], page: 1, pages: 1, total: 0 },
    additionalResponses: { "/workshop/api/custom-print-requests": [] }
  },
  {
    name: "reviews",
    pathname: "/workshop/reviews",
    expectedHeading: "Đánh giá",
    endpoint: "/workshop/api/reviews",
    response: []
  }
];

for (const scenario of existingPageScenarios) {
  test(`${scenario.name} page loads from its Workshop API endpoint`, async () => {
    const responses = {
      "/workshop/api/session": shopSession,
      [scenario.endpoint]: scenario.response,
      ...(scenario.additionalResponses || {})
    };
    const { root, fetchCalls } = await renderPage({
      pathname: scenario.pathname,
      search: "?shopId=11",
      responses
    });

    assert.match(root.innerHTML, new RegExp(scenario.expectedHeading));
    assert.doesNotMatch(root.innerHTML, /KHÔNG THỂ TẢI WORKSHOP PORTAL/);
    assert.ok(fetchCalls.some(call => call.url.pathname === scenario.endpoint));
  });
}


test("quote response posts the selected offer to the existing Workshop quote API", async () => {
  const { listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/quotes",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/quotes": {
        items: [{
          id: 5, productName: "Chi tiết nhựa", companyName: "Công ty thử nghiệm",
          quantity: 10, requiredBy: "", status: "Chờ xử lý"
        }],
        options: [[]], page: 1, pages: 1, total: 1
      },
      "/workshop/api/custom-print-requests": []
    }
  });

  const form = {
    dataset: { form: "quote", id: "5" },
    values: { amount: "150000", note: "Thời gian thực hiện 3 ngày." }
  };
  await listeners.submit({
    target: { closest: () => form },
    submitter: { value: "QUOTE" },
    preventDefault() {}
  });

  const post = fetchCalls.find(call => call.method === "POST");
  assert.ok(post);
  assert.equal(post.url.pathname, "/workshop/api/quotes/5/respond");
  const body = new URLSearchParams(post.body);
  assert.equal(body.get("action"), "QUOTE");
  assert.equal(body.get("amount"), "150000");
  assert.equal(body.get("note"), "Thời gian thực hiện 3 ngày.");
  assert.equal(post.headers["X-CSRF-Token"], "csrf-test-token");
});

test("order detail exposes a production form and posts the progress update to the correct order item", async () => {
  const { root, listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/orders",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/orders": {
        items: [{
          id: 3, code: "PK-00003", productName: "Chi tiết nhựa",
          quantity: 1, lineTotal: 50000, placedAt: "",
          orderStatus: "Đã xác nhận", paymentStatus: "Đã thanh toán"
        }],
        options: [[]], page: 1, pages: 1, total: 1
      },
      "/workshop/api/orders/3": {
        id: 3, code: "PK-00003", productName: "Chi tiết nhựa",
        orderStatus: "Đã xác nhận", paymentStatus: "Đã thanh toán",
        quantity: 1, unitPrice: 50000, orderStatusHistory: [], productionUpdates: []
      }
    }
  });

  const detailControl = { dataset: { action: "order-detail", id: "3" } };
  await listeners.click({
    target: { closest: () => detailControl }
  });
  assert.match(root.innerHTML, /Lịch sử tiến độ xưởng/);
  assert.match(root.innerHTML, /data-form="progress"/);

  const form = {
    dataset: { form: "progress", id: "3" },
    values: { stage: "ACCEPTED", progress: "0", message: "Xưởng đã tiếp nhận đơn." }
  };
  await listeners.submit({
    target: { closest: () => form },
    submitter: null,
    preventDefault() {}
  });

  const post = fetchCalls.find(call => call.method === "POST");
  assert.ok(post);
  assert.equal(post.url.pathname, "/workshop/api/orders/items/3/progress");
  const body = new URLSearchParams(post.body);
  assert.equal(body.get("stage"), "ACCEPTED");
  assert.equal(body.get("progress"), "0");
  assert.equal(body.get("message"), "Xưởng đã tiếp nhận đơn.");
  assert.equal(post.headers["X-CSRF-Token"], "csrf-test-token");
});
