export const userService = {
    login,
    logout
};

function login(username, password) {
    // store user details and basic auth credentials in local storage 
    // to keep user logged in between page refreshes
    let authdata = window.btoa(username + ':' + password);
    localStorage.setItem('user', JSON.stringify(authdata));
}

function logout() {
    // remove user from local storage to log user out
    localStorage.removeItem('user');
}
