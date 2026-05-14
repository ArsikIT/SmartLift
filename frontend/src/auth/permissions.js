export function hasRole(auth, role) {
  return auth?.roles?.includes(role) ?? false;
}

export function hasAnyRole(auth, roles) {
  return roles.some((role) => hasRole(auth, role));
}
