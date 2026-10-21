/**
 * PrintKraft — trang tài khoản khách:
 *  - Quản lý “Thiết kế 3D đã lưu” (create/update/view/duplicate/delete qua AJAX)
 *  - Bỏ/thêm yêu thích AJAX (không tải lại trang)
 *  - Toast + modal dùng chung
 *
 * Server luôn tái kiểm tra quyền sở hữu và dữ liệu; JS chỉ lo UX.
 */
(function () {
  'use strict';

  function ctx() {
    var el = document.querySelector('[data-pk-ctx]');
    return el ? el.getAttribute('data-pk-ctx') : '';
  }

  function csrf() {
    var main = document.querySelector('[data-pk-csrf]');
    return main ? main.getAttribute('data-pk-csrf') : '';
  }

  function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  async function send(url, body) {
    var params = new URLSearchParams();
    if (body) {
      Object.keys(body).forEach(function (key) {
        if (body[key] !== undefined && body[key] !== null) params.set(key, body[key]);
      });
    }
    params.set('ajax', '1');
    params.set('csrfToken', csrf());
    try {
      var res = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
          'X-Requested-With': 'XMLHttpRequest',
          'X-CSRF-Token': csrf(),
          'Accept': 'application/json'
        },
        credentials: 'same-origin',
        body: params.toString()
      });
      var payload = null;
      try { payload = await res.json(); } catch (ignore) { payload = null; }
      if (!payload) return { success: false, message: 'Máy chủ phản hồi không hợp lệ.' };
      return payload;
    } catch (e) {
      return { success: false, message: 'Không thể kết nối máy chủ. Vui lòng thử lại.' };
    }
  }

  async function getJson(url) {
    try {
      var res = await fetch(url, {
        headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
        credentials: 'same-origin'
      });
      var payload = null;
      try { payload = await res.json(); } catch (ignore) { payload = null; }
      if (!payload) return { success: false, message: 'Máy chủ phản hồi không hợp lệ.' };
      return payload;
    } catch (e) {
      return { success: false, message: 'Không thể kết nối máy chủ. Vui lòng thử lại.' };
    }
  }

  // ----------------------------------------------------------------------
  // Toast
  // ----------------------------------------------------------------------
  function toast(message, type) {
    var wrap = document.querySelector('[data-acc-toasts]');
    if (!wrap) {
      wrap = document.createElement('div');
      wrap.setAttribute('data-acc-toasts', '');
      wrap.className = 'acc-toasts';
      wrap.setAttribute('aria-live', 'polite');
      document.body.appendChild(wrap);
    }
    var item = document.createElement('div');
    item.className = 'acc-toast ' + (type === 'err' ? 'acc-toast-err' : 'acc-toast-ok');
    item.setAttribute('role', 'status');
    item.textContent = message;
    wrap.appendChild(item);
    setTimeout(function () { item.remove(); }, 4200);
  }

  // ----------------------------------------------------------------------
  // Modal helpers
  // ----------------------------------------------------------------------
  var lastFocused = null;

  function openModal(id) {
    var modal = document.getElementById(id);
    if (!modal) return;
    lastFocused = document.activeElement;
    modal.hidden = false;
    document.body.classList.add('acc-modal-open');
    var focusable = modal.querySelector('input, select, textarea, button');
    if (focusable) focusable.focus();
  }

  function closeModal(modal) {
    if (!modal) modal = document.querySelector('.acc-modal:not([hidden])');
    if (!modal) return;
    modal.hidden = true;
    if (!document.querySelector('.acc-modal:not([hidden])')) {
      document.body.classList.remove('acc-modal-open');
    }
    if (lastFocused && lastFocused.focus) lastFocused.focus();
  }

  document.addEventListener('click', function (event) {
    var closer = event.target.closest('[data-sd-close]');
    if (closer) {
      event.preventDefault();
      closeModal(closer.closest('.acc-modal'));
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') closeModal(null);
  });

  // ----------------------------------------------------------------------
  // THIẾT KẾ 3D ĐÃ LƯU
  // ----------------------------------------------------------------------
  var formModal = document.getElementById('sdFormModal');
  var detailModal = document.getElementById('sdDetailModal');
  var editingId = null;

  function field(id) { return document.getElementById(id); }

  function clearForm() {
    ['sdName', 'sdMaterial', 'sdColor', 'sdDimensions', 'sdConfig', 'sdNotes'].forEach(
      function (id) { if (field(id)) field(id).value = ''; });
    if (field('sdQuantity')) field('sdQuantity').value = '1';
    if (field('sdStatus')) field('sdStatus').value = 'DRAFT';
    if (field('sdProduct')) field('sdProduct').value = '';
    var err = document.getElementById('sdFormError');
    if (err) { err.hidden = true; err.textContent = ''; }
  }

  function fillForm(design) {
    field('sdName').value = design.designName || '';
    field('sdMaterial').value = design.material || '';
    field('sdColor').value = design.color || '';
    field('sdDimensions').value = design.dimensions || '';
    field('sdQuantity').value = design.quantity || 1;
    field('sdConfig').value = design.configJson || '';
    field('sdNotes').value = design.notes || '';
    field('sdStatus').value = design.status === 'FINAL' ? 'FINAL' : 'DRAFT';
    if (field('sdProduct')) {
      field('sdProduct').value = design.productId ? String(design.productId) : '';
    }
  }

  function showFormError(message) {
    var err = document.getElementById('sdFormError');
    if (!err) {
      err = document.createElement('div');
      err.id = 'sdFormError';
      err.className = 'notice-err';
      err.setAttribute('role', 'alert');
      formModal.querySelector('.sd-form').insertAdjacentElement('beforebegin', err);
    }
    err.textContent = message || '';
    err.hidden = !message;
  }

  document.addEventListener('click', async function (event) {
    var newBtn = event.target.closest('[data-sd-new]');
    if (newBtn) {
      editingId = null;
      clearForm();
      document.getElementById('sdFormTitle').textContent = 'Lưu thiết kế mới';
      openModal('sdFormModal');
      return;
    }

    var editBtn = event.target.closest('[data-sd-edit]');
    if (editBtn) {
      var editId = editBtn.getAttribute('data-sd-edit');
      var editResult = await getJson(ctx() + '/saved-designs?action=view&id=' + editId);
      if (!editResult.success || !editResult.data) {
        toast(editResult.message || 'Không tải được thiết kế.', 'err');
        return;
      }
      editingId = editId;
      clearForm();
      fillForm(editResult.data.design);
      document.getElementById('sdFormTitle').textContent = 'Sửa thiết kế';
      openModal('sdFormModal');
      return;
    }

    var viewBtn = event.target.closest('[data-sd-view]');
    if (viewBtn) {
      var viewId = viewBtn.getAttribute('data-sd-view');
      var viewResult = await getJson(ctx() + '/saved-designs?action=view&id=' + viewId);
      if (!viewResult.success || !viewResult.data) {
        toast(viewResult.message || 'Không tải được thiết kế.', 'err');
        return;
      }
      renderDetail(viewResult.data.design);
      openModal('sdDetailModal');
      return;
    }

    var dupBtn = event.target.closest('[data-sd-duplicate]');
    if (dupBtn) {
      var dupResult = await send(ctx() + '/saved-designs',
        { action: 'duplicate', id: dupBtn.getAttribute('data-sd-duplicate') });
      toast(dupResult.message || (dupResult.success ? 'Đã nhân bản.' : 'Thất bại.'),
        dupResult.success ? 'ok' : 'err');
      if (dupResult.success) setTimeout(function () { window.location.reload(); }, 700);
      return;
    }

    var delBtn = event.target.closest('[data-sd-delete]');
    if (delBtn) {
      var delId = delBtn.getAttribute('data-sd-delete');
      if (!window.confirm('Xóa thiết kế này? Hành động không thể hoàn tác.')) return;
      var delResult = await send(ctx() + '/saved-designs',
        { action: 'delete', id: delId });
      toast(delResult.message || (delResult.success ? 'Đã xóa.' : 'Thất bại.'),
        delResult.success ? 'ok' : 'err');
      if (delResult.success) {
        var card = document.querySelector('.sd-card[data-sd-id="' + delId + '"]');
        if (card) card.remove();
        setTimeout(function () { window.location.reload(); }, 700);
      }
      return;
    }
  });

  function row(label, value) {
    if (value === null || value === undefined || value === '') return '';
    return '<li><span>' + escapeHtml(label) + '</span><b>' + escapeHtml(value) + '</b></li>';
  }

  function renderDetail(design) {
    var body = document.getElementById('sdDetailBody');
    var html = '<ul class="acc-info">'
      + row('Tên thiết kế', design.designName)
      + row('Sản phẩm', design.productName || 'Không gắn sản phẩm')
      + row('Vật liệu', design.material)
      + row('Màu sắc', design.color)
      + row('Kích thước', design.dimensions)
      + row('Số lượng', design.quantity)
      + row('Trạng thái', design.status === 'FINAL' ? 'Hoàn tất' : 'Bản nháp')
      + row('Phiên bản', 'v' + design.version)
      + row('Ghi chú', design.notes)
      + row('Tạo lúc', design.createdAt)
      + row('Cập nhật lúc', design.updatedAt)
      + '</ul>';
    if (design.configJson) {
      html += '<p class="sd-hint" style="margin:12px 0 6px;">Cấu hình JSON đã lưu:</p>'
        + '<pre class="sd-config-view">' + escapeHtml(design.configJson) + '</pre>';
    } else {
      html += '<p class="sd-hint" style="margin-top:12px;">Thiết kế này chưa có cấu hình JSON.</p>';
    }
    if (design.productId) {
      html += '<div class="acc-modal-foot"><a class="sd-btn sd-btn-primary" href="'
        + escapeHtml(ctx() + '/product-detail?id=' + design.productId)
        + '">Tiếp tục tùy chỉnh tại sản phẩm</a></div>';
    }
    body.innerHTML = html;
  }

  var saveBtn = document.getElementById('sdSaveBtn');
  if (saveBtn) {
    saveBtn.addEventListener('click', async function () {
      var name = field('sdName').value.trim();
      if (!name) {
        showFormError('Tên thiết kế không được để trống.');
        field('sdName').focus();
        return;
      }
      showFormError('');
      saveBtn.disabled = true;
      saveBtn.textContent = 'Đang lưu…';
      var body = {
        action: editingId ? 'update' : 'create',
        id: editingId || '',
        designName: name,
        productId: field('sdProduct').value,
        material: field('sdMaterial').value.trim(),
        color: field('sdColor').value.trim(),
        dimensions: field('sdDimensions').value.trim(),
        quantity: field('sdQuantity').value,
        configJson: field('sdConfig').value.trim(),
        notes: field('sdNotes').value.trim(),
        status: field('sdStatus').value
      };
      var result = await send(ctx() + '/saved-designs', body);
      saveBtn.disabled = false;
      saveBtn.textContent = 'Lưu thiết kế';
      if (!result.success) {
        showFormError(result.message || 'Không thể lưu thiết kế.');
        return;
      }
      toast(result.message || 'Đã lưu thiết kế.', 'ok');
      setTimeout(function () { window.location.reload(); }, 600);
    });
  }

  // ----------------------------------------------------------------------
  // YÊU THÍCH — AJAX, server là nguồn chân lý
  // ----------------------------------------------------------------------
  document.addEventListener('submit', async function (event) {
    var form = event.target.closest('form[data-pk-fav]');
    if (!form) return;
    event.preventDefault();
    if (form.dataset.busy === '1') return;
    form.dataset.busy = '1';
    var btn = form.querySelector('button');
    if (btn) btn.disabled = true;

    var result = await send(form.action, {
      action: form.querySelector('input[name="action"]').value,
      productId: form.querySelector('input[name="productId"]').value
    });

    if (btn) btn.disabled = false;
    form.dataset.busy = '0';
    if (!result.success) {
      toast(result.message || 'Không thể cập nhật yêu thích.', 'err');
      return;
    }
    toast(result.message || 'Đã cập nhật danh sách yêu thích.', 'ok');

    var action = form.querySelector('input[name="action"]').value;
    if (action === 'remove') {
      var card = form.closest('.product-card');
      if (card) card.remove();
      var grid = document.querySelector('.products-grid');
      if (grid && !grid.querySelector('.product-card')) {
        window.location.reload();
      }
    } else {
      setTimeout(function () { window.location.reload(); }, 500);
    }
  });
})();
