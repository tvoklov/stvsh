import Cookies from 'js-cookie'

const session = Cookies.get('session')

export function fetchFromApi(path, params) {
    return fetch('/v1' + path, {...params, ...{
        headers: {
            session: session
        }
    }});
}