(() => {
    const type = document.getElementById('changeRequestType');
    const item = document.getElementById('changeRequestItem');
    const textGroup = document.querySelector('[data-proposal-text]');
    const addressGroup = document.querySelector('[data-proposal-address]');
    const textInput = document.getElementById('proposalText');
    const addressSelect = document.getElementById('proposalAddress');
    const output = document.getElementById('proposedValue');
    if (!type || !output) return;
    function update() {
        const isAddress = type.value === 'CHANGE_ADDRESS';
        textGroup.hidden = isAddress;
        textInput.disabled = isAddress;
        addressGroup.hidden = !isAddress;
        addressSelect.disabled = !isAddress;
        item.required = ['CHANGE_MATERIAL','CHANGE_COLOR','CHANGE_QUANTITY','REPLACE_DESIGN','CANCEL_ITEM'].includes(type.value);
        output.value = isAddress ? addressSelect.value : textInput.value.trim();
    }
    type.addEventListener('change', update);
    textInput.addEventListener('input', update);
    addressSelect.addEventListener('change', update);
    document.getElementById('changeRequestForm')?.addEventListener('submit', event => {
        update();
        if (type.value === 'CHANGE_ADDRESS' && !addressSelect.value) { event.preventDefault(); addressSelect.focus(); }
    });
    update();
})();
