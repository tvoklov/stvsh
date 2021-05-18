import Cookies from 'js-cookie'
import React from 'react'
import ReactDOM from 'react-dom'

import {fetchFromApi} from './fetching'
import {sheetTable, FoldersTable} from './folderview'
import { Sheet } from './sheet'

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
    ReactDOM.render(<FoldersTable folders={folders} onClick={f => showFolder(f.id)} />, document.getElementById("folders"));
}

function showFolder(id) {
    cleanSheet()
    fetchFromApi('/folder/' + id)
        .then(res => res.json().then(
            f => {
                fetchFromApi('/folder/' + id + '/sheets')
                    .then(res => res.json().then( sheets => {
                        ReactDOM.render(
                            sheetTable(
                                f,
                                sheets,
                                sheet => showSheet(sheet.id)
                            ),
                            document.getElementById('folder')
                        );
                    }))
            }))
}

function cleanSheet() {
    ReactDOM.render(<p></p>, document.getElementById('sheet'));
}

function showSheet(id) {
    fetchFromApi('/sheet/' + id)
        .then(res => res.json().then(
            s => {
                ReactDOM.render(
                    <Sheet sheet={s} />,
                    document.getElementById('sheet')
                );
            }
        ))
}
