/**
 * PrintKraft — logic AJAX cho các trang xác thực (đăng nhập / OTP / đăng ký /
 * quên mật khẩu). Không phụ thuộc thư viện AJAX nào khác: dùng Fetch API.
 *
 * Nguyên tắc:
 *  - Server luôn tái kiểm tra (mã OTP 111111, mật khẩu, trạng thái tài khoản);
 *    JS chỉ lo UX (loading, lỗi inline, toast).
 *  - Form vẫn submit thường nếu JS tắt (fallback trong servlet/JSP).
 *  - Mọi POST gửi kèm header X-CSRF-Token lấy từ input ẩn trong form.
 */
(function () {
  'use strict';

  // ----------------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------------
  function ctx() {
    var el = document.querySelector('[data-pk-ctx]');
    return el ? el.getAttribute('data-pk-ctx') : '';
  }

  function csrfFrom(form) {
    var input = form ? form.querySelector('input[name="csrfToken"]') : null;
    return input ? input.value : '';
  }

  function show(el) {
    if (el) { el.hidden = false; el.classList.add('is-visible'); }
  }

  function hide(el) {
    if (el) { el.hidden = true; el.classList.remove('is-visible'); }
  }

  function setLoading(btn, loading) {
    if (!btn) return;
    btn.disabled = loading;
    btn.classList.toggle('is-loading', loading);
    if (loading) {
      btn.dataset.label = btn.textContent;
      btn.textContent = 'Đang xử lý…';
    } else if (btn.dataset.label) {
      btn.textContent = btn.dataset.label;
    }
  }

  /** Hiển thị thông báo lỗi/thành công inline trong một khối form. */
  function setMessage(scope, type, text) {
    if (!scope) return;
    var box = scope.querySelector('[data-pk-message]');
    if (!box) {
      box = document.createElement('div');
      box.setAttribute('data-pk-message', '');
      box.className = 'alert';
      scope.insertBefore(box, scope.firstChild);
    }
    box.className = 'alert ' + (type === 'ok' ? 'alert-ok' : 'alert-err');
    box.textContent = text || '';
    box.hidden = !text;
    if (text) { box.setAttribute('role', type === 'ok' ? 'status' : 'alert'); }
  }

  /** Toast ngắn ở góc màn hình (Bootstrap-style, thu nhỏ — không thêm framework mới). */
  function toast(message, type) {
    var wrap = document.querySelector('[data-pk-toasts]');
    if (!wrap) {
      wrap = document.createElement('div');
      wrap.setAttribute('data-pk-toasts', '');
      wrap.className = 'pk-toasts';
      wrap.setAttribute('aria-live', 'polite');
      document.body.appendChild(wrap);
    }
    var item = document.createElement('div');
    item.className = 'pk-toast ' + (type === 'err' ? 'pk-toast-err' : 'pk-toast-ok');
    item.setAttribute('role', 'status');
    item.textContent = message;
    wrap.appendChild(item);
    setTimeout(function () {
      item.classList.add('is-out');
      setTimeout(function () { item.remove(); }, 400);
    }, 4200);
  }

  /**
   * Gửi request JSON tới servlet in-project.
   * Trả về {success, message, data, status}; không ném lỗi mạng — tự bắt và
   * báo thông báo thân thiện cho người dùng.
   */
  async function send(url, options) {
    options = options || {};
    var headers = { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' };
    if (options.csrf) headers['X-CSRF-Token'] = options.csrf;
    var init = { method: options.method || 'POST', headers: headers, credentials: 'same-origin' };
    if (options.body) {
      headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
      init.body = options.body;
    }
    try {
      var res = await fetch(url, init);
      var payload = null;
      try { payload = await res.json(); } catch (ignore) { payload = null; }
      if (!payload) {
        return { success: false, status: res.status,
          message: 'Máy chủ phản hồi không hợp lệ. Vui lòng thử lại.' };
      }
      payload.status = res.status;
      if (typeof payload.success !== 'boolean') {
        payload.success = res.ok;
      }
      return payload;
    } catch (e) {
      return { success: false, status: 0,
        message: 'Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại.' };
    }
  }

  function formData(form) {
    return new URLSearchParams(new FormData(form)).toString();
  }

  /** Chặn submit kép: đã bấm là disabled tới khi có kết quả. */
  function guard(form) {
    if (form.dataset.busy === '1') return false;
    form.dataset.busy = '1';
    return true;
  }

  function unguard(form) {
    form.dataset.busy = '0';
  }

  // ----------------------------------------------------------------------
  // Hiển thị/ẩn mật khẩu
  // ----------------------------------------------------------------------
  document.addEventListener('click', function (event) {
    var toggle = event.target.closest('[data-pk-toggle-pass]');
    if (!toggle) return;
    var input = document.getElementById(toggle.getAttribute('data-pk-toggle-pass'));
    if (!input) return;
    var revealing = input.type === 'password';
    input.type = revealing ? 'text' : 'password';
    toggle.setAttribute('aria-pressed', String(revealing));
    toggle.textContent = revealing ? 'Ẩn' : 'Hiện';
  });

  // ----------------------------------------------------------------------
  // Modal demo OAuth (Google/Facebook) — không gọi nhà cung cấp nào
  // ----------------------------------------------------------------------
  var lastFocused = null;

  function openSocialModal(provider, message) {
    var modal = document.getElementById('socialDemoModal');
    if (!modal) return;
    var text = modal.querySelector('[data-pk-social-message]');
    if (text) text.textContent = message;
    var name = modal.querySelector('[data-pk-social-name]');
    if (name) name.textContent = provider === 'facebook' ? 'Facebook' : 'Google';
    lastFocused = document.activeElement;
    modal.hidden = false;
    modal.classList.add('is-open');
    document.body.classList.add('pk-modal-open');
    var closeBtn = modal.querySelector('[data-pk-modal-close]');
    if (closeBtn) closeBtn.focus();
  }

  function closeSocialModal() {
    var modal = document.getElementById('socialDemoModal');
    if (!modal) return;
    modal.hidden = true;
    modal.classList.remove('is-open');
    document.body.classList.remove('pk-modal-open');
    if (lastFocused && lastFocused.focus) lastFocused.focus();
  }

  document.addEventListener('click', async function (event) {
    var btn = event.target.closest('[data-pk-social]');
    if (btn) {
      event.preventDefault();
      var provider = btn.getAttribute('data-pk-social');
      var fallback = provider === 'facebook'
        ? 'Đăng nhập Facebook ở chế độ demo: OAuth chưa được cấu hình nên hệ thống ' +
          'chưa kết nối tới Facebook và không đăng nhập tài khoản nào.'
        : 'Đăng nhập Google ở chế độ demo: OAuth chưa được cấu hình nên hệ thống ' +
          'chưa kết nối tới Google và không đăng nhập tài khoản nào.';
      // Hỏi server (endpoint bất hoạt) — KHÔNG có gọi ra ngoài, không đăng nhập.
      var result = await send(ctx() + '/login/' + provider,
        { method: 'GET', body: null });
      openSocialModal(provider, (result && result.message) || fallback);
      return;
    }
    if (event.target.closest('[data-pk-modal-close]') ||
        event.target.classList.contains('pk-modal-backdrop')) {
      closeSocialModal();
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') closeSocialModal();
  });

  // ----------------------------------------------------------------------
  // ĐĂNG NHẬP — mật khẩu
  // ----------------------------------------------------------------------
  var loginForm = document.querySelector('[data-pk-form="login"]');
  if (loginForm) {
    loginForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!guard(loginForm)) return;
      var btn = loginForm.querySelector('button[type="submit"]');
      setLoading(btn, true);
      setMessage(loginForm, '', '');
      var result = await send(loginForm.action,
        { body: formData(loginForm), csrf: csrfFrom(loginForm) });
      setLoading(btn, false);
      unguard(loginForm);
      if (result.success && result.data && result.data.redirect) {
        setMessage(loginForm, 'ok', 'Đăng nhập thành công. Đang chuyển hướng…');
        window.location.assign(result.data.redirect);
        return;
      }
      setMessage(loginForm, 'err', result.message || 'Đăng nhập thất bại.');
    });
  }

  // Switch sang OTP và quay lại
  var otpPanel = document.querySelector('[data-pk-panel="login-otp"]');
  var passwordPanel = document.querySelector('[data-pk-panel="login-password"]');
  document.addEventListener('click', function (event) {
    var toOtp = event.target.closest('[data-pk-show="otp"]');
    var toPassword = event.target.closest('[data-pk-show="password"]');
    if (toOtp) {
      event.preventDefault();
      if (passwordPanel) hide(passwordPanel);
      if (otpPanel) show(otpPanel);
      var first = otpPanel && otpPanel.querySelector('input');
      if (first) first.focus();
    }
    if (toPassword) {
      event.preventDefault();
      if (otpPanel) hide(otpPanel);
      if (passwordPanel) show(passwordPanel);
    }
  });

  // ----------------------------------------------------------------------
  // ĐĂNG NHẬP — OTP demo 111111
  // ----------------------------------------------------------------------
  var otpForm = document.querySelector('[data-pk-form="login-otp"]');
  if (otpForm) {
    otpForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!guard(otpForm)) return;
      // Nút submit nào được bấm sẽ mang theo action (otp_request | otp_verify),
      // nên vẫn hoạt động đúng cả khi JS tắt (browser submit thường).
      var submitter = event.submitter;
      var action = (submitter && submitter.value) || 'otp_request';
      var btn = submitter || otpForm.querySelector('button[type="submit"]');
      var emailInput = otpForm.querySelector('input[name="email"]');
      var codeInput = otpForm.querySelector('input[name="code"]');

      if (action === 'otp_request' && !emailInput.value.trim()) {
        unguard(otpForm);
        setMessage(otpForm, 'err', 'Vui lòng nhập email trước khi yêu cầu mã OTP.');
        emailInput.focus();
        return;
      }
      if (action === 'otp_verify' && (!codeInput || !codeInput.value.trim())) {
        unguard(otpForm);
        setMessage(otpForm, 'err', 'Vui lòng nhập mã OTP.');
        return;
      }

      setLoading(btn, true);
      setMessage(otpForm, '', '');
      var params = new URLSearchParams();
      params.set('action', action);
      params.set('email', emailInput.value.trim());
      if (action === 'otp_verify') params.set('code', codeInput.value.trim());
      params.set('ajax', '1');
      params.set('csrfToken', csrfFrom(otpForm));
      var result = await send(otpForm.action,
        { body: params.toString(), csrf: csrfFrom(otpForm) });
      setLoading(btn, false);
      unguard(otpForm);

      if (!result.success) {
        setMessage(otpForm, 'err', result.message || 'Yêu cầu OTP thất bại.');
        return;
      }
      if (action === 'otp_request') {
        emailInput.readOnly = true;
        var codeRow = otpForm.querySelector('[data-pk-otp-code-row]');
        if (codeRow) show(codeRow);
        var note = otpForm.querySelector('[data-pk-otp-demo-note]');
        if (note) {
          note.hidden = false;
          note.textContent = (result.data && result.data.demoOtpNote) ||
            'Chế độ demo: mã OTP kiểm thử là 111111; hệ thống chưa gửi email thật.';
        }
        var verifyBtn = otpForm.querySelector('[data-pk-otp-verify]');
        if (verifyBtn) show(verifyBtn);
        toast(result.message || 'Đã phát hành OTP demo.', 'ok');
        if (codeInput) codeInput.focus();
        return;
      }
      if (result.data && result.data.redirect) {
        setMessage(otpForm, 'ok', 'Đăng nhập thành công. Đang chuyển hướng…');
        window.location.assign(result.data.redirect);
      }
    });
  }

  // ----------------------------------------------------------------------
  // ĐĂNG KÝ — bước 1
  // ----------------------------------------------------------------------
  var registerForm = document.querySelector('[data-pk-form="register"]');
  if (registerForm) {
    registerForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!guard(registerForm)) return;
      var btn = registerForm.querySelector('button[type="submit"]');
      setLoading(btn, true);
      setMessage(registerForm, '', '');
      var params = new URLSearchParams(formData(registerForm));
      params.set('ajax', '1');
      var result = await send(registerForm.action,
        { body: params.toString(), csrf: csrfFrom(registerForm) });
      setLoading(btn, false);
      unguard(registerForm);
      if (result.success) {
        var step1 = document.querySelector('[data-pk-step="form"]');
        var step2 = document.querySelector('[data-pk-step="verify"]');
        var emailOut = document.querySelector('[data-pk-verify-email]');
        if (emailOut && result.data) emailOut.textContent = result.data.email;
        var note = document.querySelector('[data-pk-verify-note]');
        if (note && result.data) note.textContent = result.data.demoOtpNote;
        var verifyEmail = document.querySelector('input[name="email"][data-pk-verify-bound]');
        if (verifyEmail && result.data) verifyEmail.value = result.data.email;
        hide(step1);
        show(step2);
        toast(result.message || 'Đã phát hành OTP demo.', 'ok');
        var codeInput = step2 && step2.querySelector('input[name="code"]');
        if (codeInput) codeInput.focus();
        return;
      }
      setMessage(registerForm, 'err', result.message || 'Đăng ký thất bại.');
    });
  }

  // ----------------------------------------------------------------------
  // ĐĂNG KÝ — bước 2: xác thực OTP rồi tạo tài khoản
  // ----------------------------------------------------------------------
  var verifyForm = document.querySelector('[data-pk-form="register-verify"]');
  if (verifyForm) {
    verifyForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!guard(verifyForm)) return;
      var btn = verifyForm.querySelector('button[type="submit"]');
      setLoading(btn, true);
      setMessage(verifyForm, '', '');
      var params = new URLSearchParams(formData(verifyForm));
      params.set('action', 'verify');
      params.set('ajax', '1');
      var result = await send(verifyForm.action,
        { body: params.toString(), csrf: csrfFrom(verifyForm) });
      setLoading(btn, false);
      unguard(verifyForm);
      if (result.success) {
        var successBox = document.querySelector('[data-pk-step="success"]');
        var text = successBox && successBox.querySelector('[data-pk-success-text]');
        if (text) text.textContent = result.message || 'Đăng ký thành công!';
        hide(verifyForm);
        show(successBox);
        setTimeout(function () {
          window.location.assign(ctx() + '/login');
        }, 1600);
        return;
      }
      setMessage(verifyForm, 'err', result.message || 'Mã OTP không đúng.');
    });
  }

  // ----------------------------------------------------------------------
  // QUÊN MẬT KHẨU — bước 1
  // ----------------------------------------------------------------------
  var forgotForm = document.querySelector('[data-pk-form="forgot"]');
  if (forgotForm) {
    forgotForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!guard(forgotForm)) return;
      var btn = forgotForm.querySelector('button[type="submit"]');
      setLoading(btn, true);
      setMessage(forgotForm, '', '');
      var params = new URLSearchParams(formData(forgotForm));
      params.set('ajax', '1');
      var result = await send(forgotForm.action,
        { body: params.toString(), csrf: csrfFrom(forgotForm) });
      setLoading(btn, false);
      unguard(forgotForm);
      if (result.success) {
        var step1 = document.querySelector('[data-pk-step="email"]');
        var step2 = document.querySelector('[data-pk-step="reset"]');
        var emailOut = document.querySelector('[data-pk-reset-email]');
        if (emailOut && result.data) emailOut.textContent = result.data.email;
        var bound = document.querySelector('input[name="email"][data-pk-reset-bound]');
        if (bound && result.data) bound.value = result.data.email;
        var note = document.querySelector('[data-pk-reset-note]');
        if (note && result.data) note.textContent = result.data.demoOtpNote;
        var demoCode = document.querySelector('[data-pk-reset-code]');
        if (demoCode && result.data) demoCode.textContent = result.data.demoOtp;
        hide(step1);
        show(step2);
        toast(result.message || 'Đã phát hành OTP demo.', 'ok');
        var otpInput = step2 && step2.querySelector('input[name="otp"]');
        if (otpInput) otpInput.focus();
        return;
      }
      setMessage(forgotForm, 'err', result.message || 'Không thể xử lý yêu cầu.');
    });
  }

  // ----------------------------------------------------------------------
  // QUÊN MẬT KHẨU — bước 2: đặt lại mật khẩu
  // ----------------------------------------------------------------------
  var resetForm = document.querySelector('[data-pk-form="forgot-reset"]');
  if (resetForm) {
    resetForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!guard(resetForm)) return;
      var btn = resetForm.querySelector('button[type="submit"]');
      setLoading(btn, true);
      setMessage(resetForm, '', '');
      var params = new URLSearchParams(formData(resetForm));
      params.set('action', 'reset');
      params.set('ajax', '1');
      var result = await send(resetForm.action,
        { body: params.toString(), csrf: csrfFrom(resetForm) });
      setLoading(btn, false);
      unguard(resetForm);
      if (result.success) {
        setMessage(resetForm, 'ok', result.message || 'Đặt lại mật khẩu thành công.');
        setTimeout(function () {
          window.location.assign((result.data && result.data.redirect) || (ctx() + '/login'));
        }, 1200);
        return;
      }
      setMessage(resetForm, 'err', result.message || 'Không thể đặt lại mật khẩu.');
    });
  }
})();
