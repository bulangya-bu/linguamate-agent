import { onScopeDispose, shallowRef } from "vue";

export function usePhoneLayout() {
  const media = window.matchMedia("(max-width: 760px), (max-width: 960px) and (max-height: 540px) and (pointer: coarse)");
  const isPhone = shallowRef(media.matches);
  const update = () => { isPhone.value = media.matches; };
  media.addEventListener("change", update);
  onScopeDispose(() => media.removeEventListener("change", update));
  return isPhone;
}
