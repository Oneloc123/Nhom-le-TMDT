(function () {
    'use strict';

    const API = 'https://diachiviet.com/api/v1/';
    const provinceCache = new Map();

    function normalized(value) {
        return String(value || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/[đĐ]/g, 'd')
            .toLocaleLowerCase('vi')
            .replace(/\b(thanh pho|tinh|tp)\b/g, '')
            .replace(/[^a-z0-9]+/g, ' ')
            .trim();
    }

    async function fetchData(url) {
        const response = await fetch(url, { method: 'GET', mode: 'cors', credentials: 'omit', cache: 'force-cache' });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const data = await response.json();
        if (!data || !Array.isArray(data.dong)) throw new Error('Dữ liệu địa chỉ không đúng định dạng.');
        return data.dong;
    }

    function addOption(select, label, value, data) {
        const option = new Option(label, value);
        Object.keys(data || {}).forEach(function (key) {
            option.dataset[key] = String(data[key]);
        });
        select.add(option);
        return option;
    }

    function findOption(select, code, name) {
        const options = Array.from(select.options).filter(function (option) { return option.value !== ''; });
        if (code) {
            const byCode = options.find(function (option) { return option.dataset.code === String(code); });
            if (byCode) return byCode;
        }
        if (name) {
            const key = normalized(name);
            const byName = options.find(function (option) { return normalized(option.dataset.name || option.textContent) === key; });
            if (byName) return byName;
        }
        return null;
    }

    function init(root) {
        if (root.dataset.addressPickerInitialized === 'true') return;
        const provinceSelect = root.querySelector('[data-province-select]');
        const wardSelect = root.querySelector('[data-ward-select]');
        const provinceValue = root.querySelector('[data-province-value]');
        const provinceCode = root.querySelector('[data-province-code]');
        const wardValue = root.querySelector('[data-ward-value]');
        const wardCode = root.querySelector('[data-ward-code]');
        const districtValue = root.querySelector('[data-district-value]');
        const provinceFallback = root.querySelector('[data-province-fallback]');
        const wardFallback = root.querySelector('[data-ward-fallback]');
        const status = root.querySelector('[data-address-api-status]');
        if (!provinceSelect || !wardSelect || !provinceValue || !wardValue) return;
        root.dataset.addressPickerInitialized = 'true';

        const originalProvince = provinceValue.value.trim();
        const originalProvinceCode = provinceCode ? provinceCode.value.trim() : '';
        const originalWard = wardValue.value.trim();
        const originalWardCode = wardCode ? wardCode.value.trim() : '';
        let provinceChanged = false;

        function isActive() {
            const checkoutPanel = root.closest('#manualAddress');
            return !checkoutPanel || !checkoutPanel.hidden;
        }

        function setStatus(message, state) {
            if (!status) return;
            status.textContent = message;
            status.dataset.state = state || 'info';
        }

        function setProvinceValues(option) {
            const name = option ? (option.dataset.name || option.textContent.trim()) : '';
            provinceValue.value = name;
            if (provinceCode) provinceCode.value = option && !option.dataset.legacy ? (option.dataset.code || '') : '';
            if (provinceFallback && !provinceFallback.value) provinceFallback.value = name;
        }

        function setWardValues(option) {
            const name = option ? (option.dataset.name || option.textContent.trim()) : '';
            wardValue.value = name;
            if (wardCode) wardCode.value = option && !option.dataset.legacy ? (option.dataset.code || '') : '';
            if (wardFallback && !wardFallback.value) wardFallback.value = name;
        }

        function updateControlState() {
            const active = isActive();
            provinceSelect.disabled = !active || provinceSelect.hidden || !provinceSelect.options.length;
            provinceSelect.required = active && !provinceSelect.hidden;
            wardSelect.disabled = !active || wardSelect.hidden || !provinceSelect.value;
            wardSelect.required = active && !wardSelect.hidden;
            [provinceFallback, wardFallback].forEach(function (field) {
                if (field) {
                    field.disabled = !active || field.hidden;
                    field.required = active && !field.hidden;
                }
            });
        }

        function useTextFallback(message) {
            const selectedProvince = provinceSelect.selectedOptions[0];
            const selectedWard = wardSelect.selectedOptions[0];
            if (selectedProvince && selectedProvince.value) setProvinceValues(selectedProvince);
            else provinceValue.value = originalProvince;
            if (selectedWard && selectedWard.value) setWardValues(selectedWard);
            else wardValue.value = provinceChanged ? '' : originalWard;
            if (provinceFallback) {
                provinceFallback.value = provinceValue.value || originalProvince;
                provinceFallback.name = 'province';
                provinceFallback.hidden = false;
            }
            if (wardFallback) {
                wardFallback.value = wardValue.value || (provinceChanged ? '' : originalWard);
                wardFallback.name = 'ward';
                wardFallback.hidden = false;
            }
            provinceSelect.hidden = true;
            provinceSelect.disabled = true;
            provinceSelect.required = false;
            wardSelect.hidden = true;
            wardSelect.disabled = true;
            wardSelect.required = false;
            provinceValue.disabled = true;
            wardValue.disabled = true;
            if (provinceCode) provinceCode.disabled = true;
            if (wardCode) wardCode.disabled = true;
            provinceValue.removeAttribute('name');
            wardValue.removeAttribute('name');
            if (provinceCode) provinceCode.removeAttribute('name');
            if (wardCode) wardCode.removeAttribute('name');
            updateControlState();
            setStatus((message || 'Không thể tải danh mục địa chỉ tự động.') + ' Bạn có thể nhập tỉnh/thành phố và phường/xã bằng tay.', 'error');
        }

        async function loadWards(provinceOption, preserveExisting) {
            wardSelect.replaceChildren(new Option('Đang tải phường/xã…', ''));
            wardSelect.disabled = true;
            setWardValues(null);
            const slug = provinceOption ? provinceOption.dataset.slug : '';
            if (!slug) {
                if (preserveExisting && originalWard) {
                    const legacy = addOption(wardSelect, originalWard + ' (địa chỉ đã lưu)', 'legacy-ward:' + originalWard,
                        { name: originalWard, legacy: 'true' });
                    legacy.selected = true;
                    setWardValues(legacy);
                    wardSelect.disabled = !isActive();
                    setStatus('Địa chỉ đang lưu dùng tên địa giới cũ. Bạn có thể giữ nguyên hoặc chọn tỉnh/thành mới.', 'info');
                } else {
                    wardSelect.replaceChildren(new Option('Chọn tỉnh/thành phố trước', ''));
                    wardSelect.disabled = true;
                }
                updateControlState();
                return;
            }

            try {
                let rows = provinceCache.get(slug);
                if (!rows) {
                    rows = await fetchData(API + 'xa/' + encodeURIComponent(slug) + '.json');
                    provinceCache.set(slug, rows);
                }
                wardSelect.replaceChildren(new Option('Chọn phường / xã', ''));
                rows.forEach(function (row) {
                    if (!row || !row.ma_xa || !row.ten) return;
                    addOption(wardSelect, row.ten, String(row.ma_xa), {
                        name: row.ten,
                        code: String(row.ma_xa),
                        postalCode: row.ma_buu_chinh || ''
                    });
                });
                let selected = preserveExisting ? findOption(wardSelect, originalWardCode, originalWard) : null;
                if (!selected && preserveExisting && originalWard) {
                    selected = addOption(wardSelect, originalWard + ' (địa chỉ đã lưu)', 'legacy-ward:' + originalWard,
                        { name: originalWard, legacy: 'true' });
                }
                if (selected) selected.selected = true;
                setWardValues(selected || null);
                wardSelect.disabled = !isActive() || !provinceSelect.value;
                if (selected) {
                    setStatus('Đã tải danh mục tỉnh/thành và phường/xã.', 'success');
                } else {
                    setStatus('Chọn đúng phường/xã theo địa chỉ giao hàng.', 'success');
                }
                updateControlState();
            } catch (error) {
                useTextFallback('Dịch vụ địa chỉ tạm thời không phản hồi.');
            }
        }

        provinceSelect.addEventListener('change', async function () {
            provinceChanged = true;
            const option = provinceSelect.selectedOptions[0];
            setProvinceValues(option && option.value ? option : null);
            if (provinceFallback) provinceFallback.value = provinceValue.value;
            if (wardFallback) wardFallback.value = '';
            if (wardCode) wardCode.value = '';
            if (districtValue) districtValue.value = '';
            wardValue.value = '';
            await loadWards(option && option.value ? option : null, false);
            updateControlState();
        });

        wardSelect.addEventListener('change', function () {
            setWardValues(wardSelect.selectedOptions[0] && wardSelect.value ? wardSelect.selectedOptions[0] : null);
            if (districtValue) districtValue.value = '';
        });

        root.clearAddressSelection = function () {
            if (provinceFallback) { provinceFallback.value = ''; }
            if (wardFallback) { wardFallback.value = ''; }
            if (districtValue) districtValue.value = '';
            if (!provinceFallback || provinceFallback.hidden) {
                provinceSelect.hidden = false;
                provinceSelect.value = '';
                setProvinceValues(null);
            } else {
                provinceFallback.value = '';
                provinceFallback.hidden = false;
                provinceFallback.disabled = !isActive();
            }
            if (!wardFallback || wardFallback.hidden) {
                wardSelect.hidden = false;
                wardSelect.replaceChildren(new Option('Chọn tỉnh/thành phố trước', ''));
                wardSelect.disabled = true;
                setWardValues(null);
            } else {
                wardFallback.value = '';
                wardFallback.hidden = false;
                wardFallback.disabled = !isActive();
            }
            if (provinceCode) { provinceCode.value = ''; provinceCode.name = 'provinceCode'; provinceCode.disabled = false; }
            if (wardCode) { wardCode.value = ''; wardCode.name = 'wardCode'; wardCode.disabled = false; }
            provinceValue.name = 'province';
            wardValue.name = 'ward';
            provinceValue.disabled = false;
            wardValue.disabled = false;
            updateControlState();
        };

        root.selectAddressSelection = async function (values) {
            if (root.addressPickerReady) await root.addressPickerReady;
            const provinceFallbackActive = provinceFallback && !provinceFallback.hidden;
            const wardFallbackActive = wardFallback && !wardFallback.hidden;
            if (provinceFallbackActive || wardFallbackActive) {
                if (provinceFallback) {
                    provinceFallback.value = values.province || '';
                    provinceFallback.hidden = false;
                    provinceFallback.name = 'province';
                    provinceFallback.disabled = !isActive();
                }
                if (wardFallback) {
                    wardFallback.value = values.ward || '';
                    wardFallback.hidden = false;
                    wardFallback.name = 'ward';
                    wardFallback.disabled = !isActive();
                }
                provinceValue.removeAttribute('name');
                wardValue.removeAttribute('name');
                if (provinceCode) provinceCode.removeAttribute('name');
                if (wardCode) wardCode.removeAttribute('name');
                setStatus('Không thể tải danh mục địa chỉ tự động. Bạn có thể tiếp tục bằng cách nhập địa chỉ thủ công.', 'error');
                return;
            }
            const provinceOption = findOption(provinceSelect, values.provinceCode, values.province);
            if (!provinceOption) {
                if (provinceFallback) {
                    provinceFallback.value = values.province || '';
                    provinceFallback.hidden = false;
                    provinceFallback.name = 'province';
                    provinceFallback.disabled = !isActive();
                }
                if (wardFallback) {
                    wardFallback.value = values.ward || '';
                    wardFallback.hidden = false;
                    wardFallback.name = 'ward';
                    wardFallback.disabled = !isActive();
                }
                provinceSelect.hidden = true;
                wardSelect.hidden = true;
                provinceValue.removeAttribute('name');
                wardValue.removeAttribute('name');
                if (provinceCode) provinceCode.removeAttribute('name');
                if (wardCode) wardCode.removeAttribute('name');
                updateControlState();
                return;
            }
            provinceSelect.hidden = false;
            wardSelect.hidden = false;
            provinceOption.selected = true;
            setProvinceValues(provinceOption);
            await loadWards(provinceOption, false);
            let wardOption = findOption(wardSelect, values.wardCode, values.ward);
            if (!wardOption && values.ward) {
                wardOption = addOption(wardSelect, values.ward + ' (địa chỉ đã lưu)',
                    'legacy-ward:' + values.ward, { name: values.ward, legacy: 'true' });
            }
            if (wardOption) wardOption.selected = true;
            setWardValues(wardOption || null);
            if (districtValue) districtValue.value = values.district || '';
            updateControlState();
        };

        root.addressPickerReady = (async function loadProvinces() {
            setStatus('Đang tải danh mục tỉnh/thành…', 'info');
            try {
                const rows = await fetchData(API + 'tinh.json');
                provinceSelect.replaceChildren(new Option('Chọn tỉnh / thành phố', ''));
                rows.forEach(function (row) {
                    if (!row || !row.ma_tinh || !row.ten) return;
                    let slug = '';
                    try {
                        const url = new URL(row.url);
                        if (url.hostname === 'diachiviet.com' || url.hostname.endsWith('.diachiviet.com')) {
                            const segments = url.pathname.split('/').filter(Boolean);
                            slug = segments[segments.length - 1] || '';
                        }
                    } catch (ignored) { }
                    const name = row.ten_day_du || row.ten;
                    addOption(provinceSelect, name, String(row.ma_tinh), {
                        name: name,
                        code: String(row.ma_tinh),
                        slug: slug
                    });
                });
                let selected = findOption(provinceSelect, originalProvinceCode, originalProvince);
                if (!selected && originalProvince) {
                    selected = addOption(provinceSelect, originalProvince + ' (địa chỉ đã lưu)',
                        'legacy-province:' + originalProvince, { name: originalProvince, legacy: 'true' });
                }
                if (selected) selected.selected = true;
                setProvinceValues(selected || null);
                await loadWards(selected || null, Boolean(selected && originalWard));
                if (!selected) {
                    setStatus('Chọn tỉnh/thành phố trước, sau đó chọn phường/xã.', 'info');
                    wardSelect.disabled = true;
                }
                updateControlState();
            } catch (error) {
                useTextFallback('Dịch vụ địa chỉ tạm thời không phản hồi.');
            }
        })();
    }

    document.querySelectorAll('[data-vn-address-picker]').forEach(init);
})();
