(function () {
    'use strict';
    const header = document.getElementById('siteHeader');
    const toggle = document.getElementById('navToggle');
    const nav = document.getElementById('mainNav');
    if (!header || !toggle || !nav) return;

    function closeMenu() {
        header.classList.remove('nav-open');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.setAttribute('aria-label', 'Mở trình đơn');
    }
    toggle.addEventListener('click', function () {
        const open = !header.classList.contains('nav-open');
        header.classList.toggle('nav-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Đóng trình đơn' : 'Mở trình đơn');
    });
    nav.addEventListener('click', function (event) {
        if (event.target.closest('a')) closeMenu();
    });
    document.addEventListener('click', function (event) {
        if (!header.contains(event.target)) closeMenu();
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeMenu();
            toggle.focus();
        }
    });

    // Đồng bộ trạng thái active của menu điều hướng
    function syncActiveNav() {
        try {
            const path = window.location.pathname.replace(/\/+$/, '');
            const search = decodeURIComponent(window.location.search || '');
            const links = Array.from(nav.querySelectorAll('a[href]'));
            if (!links.length) return;

            // 1. Kiểm tra trường hợp tab Doanh nghiệp qua query param
            if (search.includes('category=') && search.includes('doanh')) {
                const enterpriseLink = links.find(a => (a.getAttribute('href') || '').includes('category='));
                if (enterpriseLink) {
                    links.forEach(l => {
                        l.classList.remove('is-active', 'active');
                        l.removeAttribute('aria-current');
                    });
                    enterpriseLink.classList.add('is-active', 'active');
                    enterpriseLink.setAttribute('aria-current', 'page');
                    return;
                }
            }

            // 2. Tìm link khớp đường dẫn
            let targetLink = null;
            for (const a of links) {
                const href = a.getAttribute('href') || '';
                const cleanHref = href.split('?')[0].split('#')[0].replace(/\/+$/, '');
                if (!cleanHref) continue;

                if (cleanHref.endsWith('/products')) {
                    if (path === cleanHref || path.endsWith('/products') || path.includes('/product-detail')) {
                        targetLink = a;
                        break;
                    }
                } else if (cleanHref.endsWith('/design')) {
                    if ((path === cleanHref || path.endsWith('/design') || path.includes('/design-request')) && !path.includes('/my-designs')) {
                        targetLink = a;
                        break;
                    }
                } else if (cleanHref.endsWith('/stores')) {
                    if (path === cleanHref || path.endsWith('/stores')) {
                        targetLink = a;
                        break;
                    }
                } else if (cleanHref.endsWith('/quotes')) {
                    if (path === cleanHref || path.endsWith('/quotes')) {
                        targetLink = a;
                        break;
                    }
                } else if (cleanHref.endsWith('/favorites')) {
                    if (path === cleanHref || path.endsWith('/favorites')) {
                        targetLink = a;
                        break;
                    }
                } else if (cleanHref.endsWith('/purchase-history')) {
                    if (path === cleanHref || path.endsWith('/purchase-history') || path.includes('/order-detail') || path.includes('/order-tracking') || path.includes('/order-change-request')) {
                        targetLink = a;
                        break;
                    }
                }
            }

            // 3. Nếu chưa khớp tab nào khác, kiểm tra Trang chủ
            if (!targetLink) {
                const homeLink = links.find(a => (a.getAttribute('href') || '').includes('/home'));
                if (homeLink) {
                    const homeHref = (homeLink.getAttribute('href') || '').split('?')[0].replace(/\/+$/, '');
                    const ctxRoot = homeHref.replace(/\/home$/, '');
                    if (path === '' || path === '/' || path === ctxRoot || path.endsWith('/home') || path.endsWith('/index.jsp')) {
                        targetLink = homeLink;
                    }
                }
            }

            if (targetLink) {
                links.forEach(l => {
                    l.classList.remove('is-active', 'active');
                    l.removeAttribute('aria-current');
                });
                targetLink.classList.add('is-active', 'active');
                targetLink.setAttribute('aria-current', 'page');
            }
        } catch (e) {}
    }

    syncActiveNav();
})();
