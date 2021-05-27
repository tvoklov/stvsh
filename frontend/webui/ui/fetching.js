import { param } from 'jquery';
import Cookies from 'js-cookie'

const session = Cookies.get('session')

export function fetchFromApi(path, params) {
    return fetch('/v1' + path, {...params, ...{
        headers: {
            session: session
        }
    }});
}

export function postToApi(path, body, params, headers) {
    return fetch('/v1' + path, {...params, ...{
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            session: session
        },
        body: JSON.stringify(body)
    }});
}

export function putToApi(path, body, params) {
    return fetch('/v1' + path, {...param, ...{
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            session: session
        },
        body: JSON.stringify(body)
    }});
}