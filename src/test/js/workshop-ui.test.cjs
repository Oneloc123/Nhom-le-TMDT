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
  const intervalCallbacks = [];
  const navLinks = {};
  for (const key of ["orders", "quotes", "customPrintRequests", "messages", "comments", "changeRequests"]) {
    let badge = null;
    navLinks[key] = {
      dataset: { navKey: key },
      querySelector(selector) { return selector === ".nav-alert" ? badge : null; },
      appendChild(node) { badge = node; },
      removeChild(node) { if (badge === node) badge = null; },
      get badge() { return badge; }
    };
  }
  let html = "";
  const root = {
    dataset: { contextPath: "" },
    htmlWrites: 0,
    get innerHTML() { return html; },
    set innerHTML(value) { html = String(value); this.htmlWrites += 1; },
    addEventListener(type, callback) {
      listeners[type] = callback;
    },
    querySelector() {
      return null;
    },
    querySelectorAll(selector) {
      return selector === "[data-nav-key]" ? Object.values(navLinks) : [];
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
    },
    createElement(tagName) {
      return {
        tagName,
        className: "",
        textContent: "",
        attributes: {},
        setAttribute(name, value) { this.attributes[name] = String(value); }
      };
    }
  };
  const window = {
    location: {
      origin: "http://localhost",
      pathname,
      search,
      href: `http://localhost${pathname}${search}`,
      assign(url) {
        this.href = String(url);
      }
    },
    history: {
      pushState(state, title, url) {
        const parsed = new URL(String(url), "http://localhost");
        window.location.pathname = parsed.pathname;
        window.location.search = parsed.search;
        window.location.href = parsed.href;
      }
    },
    addEventListener() {},
    setTimeout() {
      return 1;
    },
    setInterval(callback) {
      intervalCallbacks.push(callback);
      return intervalCallbacks.length;
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

  return { root, listeners, fetchCalls, toast, intervalCallbacks, navLinks };
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
    response: { items: [], options: [[]], page: 1, pages: 1, total: 0 },
    additionalResponses: { "/workshop/api/custom-print-pricing": [] }
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
  },
  {
    name: "comments",
    pathname: "/workshop/comments",
    expectedHeading: "Hỏi đáp & Bình luận sản phẩm",
    endpoint: "/workshop/api/comments",
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

    assert.match(root.innerHTML, new RegExp(scenario.expectedHeading.replace(/&/g, "&amp;")));
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
  assert.match(root.innerHTML, /Tiếp nhận · 0%/);
  assert.match(root.innerHTML, /Đang in · 55%/);
  assert.match(root.innerHTML, /Gia công sau in · 85%/);
  assert.match(root.innerHTML, /Sẵn sàng giao · 100%/);
  assert.doesNotMatch(root.innerHTML, /name="progress"/);
  assert.match(root.innerHTML, /phần trăm được hệ thống tự gán/i);

  const form = {
    dataset: { form: "progress", id: "3" },
    values: { stage: "ACCEPTED", message: "Xưởng đã tiếp nhận đơn." }
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
  assert.equal(body.has("progress"), false);
  assert.equal(body.get("message"), "Xưởng đã tiếp nhận đơn.");
  assert.equal(post.headers["X-CSRF-Token"], "csrf-test-token");
});

test("comments page renders questions and quick-reply form", async () => {
  const { root } = await renderPage({
    pathname: "/workshop/comments",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/comments": [{
        id: 101,
        productId: 5,
        productName: "Đế điện thoại mô-đun",
        customerName: "Nguyễn Văn A",
        content: "Mẫu này in bằng nhựa nào bền nhất vậy xưởng?",
        createdAt: "16/11/2026 10:00",
        answered: false,
        status: "UNANSWERED",
        replies: []
      }]
    }
  });

  assert.match(root.innerHTML, /Đế điện thoại mô-đun/);
  assert.match(root.innerHTML, /Mẫu này in bằng nhựa nào bền nhất/);
  assert.match(root.innerHTML, /data-form="comment-reply"/);
  assert.match(root.innerHTML, /Chờ phản hồi/);
});

test("comments reply form posts reply to /comments/:id/reply", async () => {
  const { listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/comments",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/comments": [{
        id: 101,
        productId: 5,
        productName: "Đế điện thoại mô-đun",
        customerName: "Nguyễn Văn A",
        content: "Mẫu này in bằng nhựa nào bền nhất vậy xưởng?",
        createdAt: "16/11/2026 10:00",
        answered: false,
        status: "UNANSWERED",
        replies: []
      }]
    }
  });

  const form = {
    dataset: { form: "comment-reply", id: "101" },
    values: { content: "Dạ mẫu này in bằng nhựa PETG hoặc ABS là bền và cứng cáp nhất ạ." }
  };
  await listeners.submit({
    target: { closest: () => form },
    submitter: null,
    preventDefault() {}
  });

  const post = fetchCalls.find(call => call.method === "POST");
  assert.ok(post);
  assert.equal(post.url.pathname, "/workshop/api/comments/101/reply");
  const body = new URLSearchParams(post.body);
  assert.equal(body.get("content"), "Dạ mẫu này in bằng nhựa PETG hoặc ABS là bền và cứng cáp nhất ạ.");
  assert.equal(post.headers["X-CSRF-Token"], "csrf-test-token");
});

test("workshop navigation shows actionable red badges from the notification summary", async () => {
  const { root, fetchCalls } = await renderPage({
    pathname: "/workshop/dashboard",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/dashboard": {
        shop: { verificationStatus: "VERIFIED", status: "ACTIVE" },
        metrics: {}, recentOrders: [], recentQuotes: []
      },
      "/workshop/api/notification-summary": {
        pendingOrders: 4,
        pendingQuotes: 2,
        pendingCustomPrints: 1,
        pendingChanges: 3,
        unreadMessages: 5,
        unansweredComments: 6
      }
    }
  });

  assert.match(root.innerHTML, /Đơn hàng &amp; tiến độ<\/span><span class="nav-alert"[^>]*>4<\/span>/);
  assert.match(root.innerHTML, /Yêu cầu báo giá<\/span><span class="nav-alert"[^>]*>2<\/span>/);
  assert.match(root.innerHTML, /Yêu cầu in 3D<\/span><span class="nav-alert"[^>]*>1<\/span>/);
  assert.match(root.innerHTML, /Tin nhắn theo đơn hàng<\/span><span class="nav-alert"[^>]*>5<\/span>/);
  assert.match(root.innerHTML, /Hỏi đáp &amp; Bình luận<\/span><span class="nav-alert"[^>]*>6<\/span>/);
  assert.match(root.innerHTML, /Yêu cầu thay đổi<\/span><span class="nav-alert"[^>]*>3<\/span>/);
  assert.ok(fetchCalls.some(call => call.url.pathname === "/workshop/api/notification-summary"));
});

test("clicking sidebar nav-link transitions page via SPA navigation without full reload", async () => {
  const { root, listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/dashboard",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/dashboard": {
        shop: { verificationStatus: "VERIFIED", status: "ACTIVE", rating: 4.8 },
        metrics: {}, recentOrders: [], recentQuotes: []
      },
      "/workshop/api/comments": [{
        id: 101,
        productId: 5,
        productName: "Đế điện thoại mô-đun",
        customerName: "Nguyễn Văn A",
        content: "Mẫu này in bằng nhựa nào?",
        createdAt: "16/11/2026 10:00",
        answered: false,
        status: "UNANSWERED",
        replies: []
      }]
    }
  });

  assert.match(root.innerHTML, /Tổng quan xưởng/);

  const navLink = {
    getAttribute(attr) {
      if (attr === "href") return "/workshop/comments?shopId=11";
      return null;
    },
    href: "http://localhost/workshop/comments?shopId=11"
  };
  let prevented = false;
  await listeners.click({
    target: { closest: selector => selector.includes("a.nav-link") ? navLink : null },
    preventDefault() { prevented = true; },
    button: 0
  });

  assert.equal(prevented, true);
  for (let attempt = 0; attempt < 10; attempt += 1) {
    await new Promise(resolve => setImmediate(resolve));
    if (root.innerHTML.includes("Hỏi đáp &amp; Bình luận sản phẩm")) break;
  }
  assert.match(root.innerHTML, /Hỏi đáp &amp; Bình luận sản phẩm/);
  assert.ok(fetchCalls.some(call => call.url.pathname === "/workshop/api/comments"));
});

test("selecting message thread updates conversation in-place without page reload", async () => {
  const { root, listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/messages",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/messages": [
        { orderId: 42, orderCode: "PK-00042", customerName: "Khách Một", unread: 1, lastMessage: "Chào xưởng" },
        { orderId: 43, orderCode: "PK-00043", customerName: "Khách Hai", unread: 0, lastMessage: "Hàng xong chưa" }
      ],
      "/workshop/api/messages/43": [
        { id: 1, senderRole: "CUSTOMER", sender: "Khách Hai", body: "Hàng xong chưa", createdAt: "20/11/2026 10:00", mine: false }
      ],
      "/workshop/api/messages/43/escalation": null
    }
  });

  assert.match(root.innerHTML, /PK-00042/);
  assert.match(root.innerHTML, /PK-00043/);
  assert.match(root.innerHTML, /Chọn một hội thoại/);

  const threadLink = {
    getAttribute(attr) {
      if (attr === "href") return "/workshop/messages?shopId=11&orderId=43";
      return null;
    },
    href: "http://localhost/workshop/messages?shopId=11&orderId=43"
  };
  let prevented = false;
  await listeners.click({
    target: { closest: selector => selector.includes("a.message-thread") ? threadLink : null },
    preventDefault() { prevented = true; },
    button: 0
  });

  assert.equal(prevented, true);
  for (let attempt = 0; attempt < 10; attempt += 1) {
    await new Promise(resolve => setImmediate(resolve));
    if (root.innerHTML.includes("Hàng xong chưa")) break;
  }
  assert.match(root.innerHTML, /PK-00043/);
  assert.match(root.innerHTML, /Khách Hai/);
  assert.match(root.innerHTML, /Hàng xong chưa/);
  assert.ok(fetchCalls.some(call => call.url.pathname === "/workshop/api/messages/43"));
});


test("custom 3D print request list is a separate page with clear actions", async () => {
  const { root, fetchCalls } = await renderPage({
    pathname: "/workshop/custom-print-requests",
    search: "?shopId=11&status=PENDING_REVIEW",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/custom-print-requests": [{
        id: 91,
        request_code: "CP-00091",
        status: "PENDING_REVIEW",
        original_filename: "customer-part.obj",
        customer_name: "Nguyễn Văn A",
        customer_email: "a@example.test",
        technology: "FDM",
        material: "PLA",
        quantity: 3,
        width_mm: 40,
        depth_mm: 20,
        height_mm: 12,
        estimated_total: 125000,
        created_at: "2026-11-25 08:30:00"
      }]
    }
  });

  assert.match(root.innerHTML, /Yêu cầu in 3D/);
  assert.match(root.innerHTML, /customer-part\.obj/);
  assert.match(root.innerHTML, /Nguyễn Văn A/);
  assert.match(root.innerHTML, /Xem chi tiết/);
  assert.match(root.innerHTML, /Tải OBJ/);
  assert.doesNotMatch(root.innerHTML, /data-form="custom-print-quote"/);
  assert.ok(fetchCalls.some(call => call.url.pathname === "/workshop/api/custom-print-requests"));
  assert.equal(fetchCalls.some(call => call.url.pathname === "/workshop/api/quotes"), false);
});

test("custom 3D print detail page loads one request, displays the 3D viewer and quote editor", async () => {
  const { root, fetchCalls } = await renderPage({
    pathname: "/workshop/custom-print-requests/91",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/custom-print-requests/91": {
        id: 91,
        request_code: "CP-00091",
        status: "PENDING_REVIEW",
        original_filename: "customer-part.obj",
        file_size_bytes: 2048,
        customer_name: "Nguyễn Văn A",
        customer_email: "a@example.test",
        customer_phone: "0900000000",
        technology: "FDM",
        material: "PLA",
        quantity: 3,
        infill_percent: 20,
        layer_height_mm: 0.2,
        width_mm: 40,
        depth_mm: 20,
        height_mm: 12,
        volume_cm3: 2.5,
        analysis_status: "ANALYZED",
        source_unit: "mm",
        estimated_total: 125000,
        quoted_amount: null,
        quote_valid_until: "2026-11-28 08:30:00",
        customer_note: "Xin giữ bề mặt mịn.",
        color_option: "WHITE",
        post_processing_option: "SANDING",
        created_at: "2026-11-25 08:30:00"
      }
    }
  });

  assert.match(root.innerHTML, /Chi tiết yêu cầu in 3D/);
  assert.match(root.innerHTML, /Mô hình 3D khách hàng gửi/);
  assert.match(root.innerHTML, /custom-print-model-canvas/);
  assert.match(root.innerHTML, /Tải file OBJ/);
  assert.match(root.innerHTML, /data-form="custom-print-quote"/);
  assert.match(root.innerHTML, /Gửi báo giá cho khách/);
  assert.ok(fetchCalls.some(call => call.url.pathname === "/workshop/api/custom-print-requests/91"));
});

test("custom 3D print detail quote form submits to the request quote endpoint", async () => {
  const { listeners, fetchCalls } = await renderPage({
    pathname: "/workshop/custom-print-requests/91",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/custom-print-requests/91": {
        id: 91, request_code: "CP-00091", status: "PENDING_REVIEW",
        original_filename: "customer-part.obj", customer_name: "Nguyễn Văn A",
        technology: "FDM", material: "PLA", quantity: 1,
        created_at: "2026-11-25 08:30:00"
      }
    }
  });

  const form = {
    dataset: { form: "custom-print-quote", id: "91" },
    values: { amount: "150000", productionDays: "4", note: "Hoàn thiện và giao sau bốn ngày." }
  };
  await listeners.submit({
    target: { closest: () => form },
    submitter: { value: "QUOTE" },
    preventDefault() {}
  });

  const post = fetchCalls.find(call => call.method === "POST");
  assert.ok(post);
  assert.equal(post.url.pathname, "/workshop/api/custom-print-requests/91/quote");
  const body = new URLSearchParams(post.body);
  assert.equal(body.get("amount"), "150000");
  assert.equal(body.get("productionDays"), "4");
  assert.equal(body.get("note"), "Hoàn thiện và giao sau bốn ngày.");
  assert.equal(post.headers["X-CSRF-Token"], "csrf-test-token");
});

test("workshop logout form submits POST with the page CSRF token", async () => {
  const { root } = await renderPage({
    pathname: "/workshop/dashboard",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/dashboard": {
        shop: { verificationStatus: "VERIFIED", status: "ACTIVE" },
        metrics: {}, recentOrders: [], recentQuotes: []
      }
    }
  });

  assert.match(root.innerHTML, /id="workshop-logout-form"/);
  assert.match(root.innerHTML, /method="post" action="\/logout"/);
  assert.match(root.innerHTML, /name="csrfToken" value="csrf-test-token"/);
});


test("notification polling updates sidebar badges without rerendering product forms", async () => {
  const responses = {
    "/workshop/api/session": shopSession,
    "/workshop/api/offers": {
      items: [{
        id: 8, productId: 18, name: "Móc khóa in 3D", category: "Phụ kiện",
        moderationStatus: "APPROVED", offerActive: true, unitPrice: 25000,
        minimumQuantity: 1, productionDays: 2, stockStatus: "IN_STOCK",
        printingMethod: "FDM", material: "PLA", canEditContent: true
      }],
      options: [[{ id: 1, name: "Phụ kiện" }]],
      page: 1, pages: 1, total: 1
    },
    "/workshop/api/custom-print-pricing": [{
      profile_id: 4, printing_method: "FDM", material: "PLA", is_active: true,
      minimum_order_price: 25000, production_days: 3,
      material_price_per_gram: 180, machine_cost_per_hour: 10000,
      deposition_rate_cm3_per_hour: 10, minimum_layer_height_mm: 0.08,
      maximum_layer_height_mm: 0.4, options: [], discounts: []
    }],
    "/workshop/api/notification-summary": {
      pendingOrders: 0, pendingQuotes: 0, pendingCustomPrints: 1,
      pendingChanges: 0, unreadMessages: 0, unansweredComments: 0
    }
  };
  const { root, intervalCallbacks, navLinks, fetchCalls } = await renderPage({
    pathname: "/workshop/products",
    search: "?shopId=11",
    responses
  });

  assert.match(root.innerHTML, /offer-create-editor/);
  assert.match(root.innerHTML, /offer-editor/);
  assert.match(root.innerHTML, /price-create-editor/);
  assert.match(root.innerHTML, /price-profile-card/);
  assert.ok(navLinks.customPrintRequests.badge);
  assert.equal(navLinks.customPrintRequests.badge.textContent, "1");

  const writesBeforePolling = root.htmlWrites;
  const htmlBeforePolling = root.innerHTML;
  responses["/workshop/api/notification-summary"] = {
    pendingOrders: 2, pendingQuotes: 1, pendingCustomPrints: 4,
    pendingChanges: 0, unreadMessages: 3, unansweredComments: 0
  };
  assert.equal(intervalCallbacks.length, 1);
  await intervalCallbacks[0]();
  await new Promise(resolve => setImmediate(resolve));

  assert.equal(root.htmlWrites, writesBeforePolling);
  assert.equal(root.innerHTML, htmlBeforePolling);
  assert.equal(navLinks.customPrintRequests.badge.textContent, "4");
  assert.equal(navLinks.orders.badge.textContent, "2");
  assert.equal(navLinks.quotes.badge.textContent, "1");
  assert.equal(navLinks.messages.badge.textContent, "3");
  assert.ok(fetchCalls.some(call => call.url.pathname === "/workshop/api/notification-summary"));
});

test("product and custom print pricing screens render clearer management sections", async () => {
  const { root } = await renderPage({
    pathname: "/workshop/products",
    search: "?shopId=11",
    responses: {
      "/workshop/api/session": shopSession,
      "/workshop/api/offers": {
        items: [{
          id: 8, productId: 18, name: "Móc khóa in 3D", category: "Phụ kiện",
          moderationStatus: "APPROVED", offerActive: true, unitPrice: 25000,
          minimumQuantity: 1, productionDays: 2, stockStatus: "IN_STOCK",
          printingMethod: "FDM", material: "PLA", canEditContent: true
        }],
        options: [[{ id: 1, name: "Phụ kiện" }]],
        page: 1, pages: 1, total: 1
      },
      "/workshop/api/custom-print-pricing": [{
        profile_id: 4, printing_method: "FDM", material: "PLA", is_active: true,
        minimum_order_price: 25000, production_days: 3,
        material_price_per_gram: 180, machine_cost_per_hour: 10000,
        deposition_rate_cm3_per_hour: 10, minimum_layer_height_mm: 0.08,
        maximum_layer_height_mm: 0.4, options: [], discounts: []
      }]
    }
  });

  assert.match(root.innerHTML, /Đang chào bán/);
  assert.match(root.innerHTML, /Chờ Admin duyệt/);
  assert.match(root.innerHTML, /offer-price-grid/);
  assert.match(root.innerHTML, /Tình trạng kho/);
  assert.match(root.innerHTML, /price-summary-grid/);
  assert.match(root.innerHTML, /Đang nhận đơn trực tiếp/);
  assert.match(root.innerHTML, /Thêm \/ cập nhật tùy chọn màu hoặc gia công/);
  assert.match(root.innerHTML, /Thêm \/ cập nhật chiết khấu số lượng/);
});
