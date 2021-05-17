import Cookies from 'js-cookie'
import React from 'react'
import ReactDOM from 'react-dom'

import {fetchFromApi} from './fetching'
import {sheetTable, foldersTable} from './folderview'

const session = Cookies.get('session')
let user = Cookies.get('user')

if (session === undefined || session === "") {
    const badSession = (
        <section>
            <h1>No session in cookies</h1>
        </section>
    );
    ReactDOM.render(badSession, document.getElementById("folders"));
} else {
    fetchFromApi('/user/'+ user + "/folders", {})
        .then(res => {
            res.json().then(showFolders)
        })
}

function showFolders(folders) {
    ReactDOM.render(foldersTable(folders, f => showFolder(f.id)), document.getElementById("folders"));
}

function showFolder(id) {
    fetchFromApi('/folder/' + id)
        .then(res => res.json().then(
            f => {
                fetchFromApi('/folder/' + id + '/sheets')
                    .then(res => res.json().then( sheets => {
                        ReactDOM.render(
                            sheetTable(
                                f,
                                sheets,
                                sheet =>
                                    console.log('clicked ' + sheet.id)
                            ),
                            document.getElementById('folder')
                        );
                    }))
            }))
}
