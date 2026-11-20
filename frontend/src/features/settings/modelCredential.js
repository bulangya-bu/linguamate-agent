import { computed, shallowRef, watch } from 'vue';

export function normalizeAddress(value = '') {
  const address = value.trim().replace(/\/+$/, '');
  try {
    return new URL(address).pathname === '/' ? address + '/v1' : address;
  } catch {
    return address;
  }
}

export function useModelCredential(form) {
  const saved = shallowRef(null);
  const address = computed(() => normalizeAddress(form.baseUrl));
  const keyHint = computed(() => saved.value?.hasApiKey && address.value === saved.value.address
    ? '留空可沿用此地址的密钥' : '填写此服务的 API 密钥');

  watch(address, () => {
    form.apiKey = '';
    form.models = [];
    form.status = '';
  }, { flush: 'sync' });

  function rememberCredential(settings) {
    saved.value = { address: normalizeAddress(settings.baseUrl), hasApiKey: settings.hasApiKey };
  }
  return { keyHint, rememberCredential };
}
