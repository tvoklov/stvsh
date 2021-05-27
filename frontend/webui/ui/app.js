import Cookies from 'js-cookie'
import React from 'react'
import ReactDOM from 'react-dom'

import { fetchFromApi, postToApi, putToApi } from './fetching'
import { FolderTable } from './folder/FolderTable'
import { SheetTable } from './sheet/SheetTable'
import { PrettySheetView } from './sheet/view/PrettySheetView'
import { EditSheetView } from './sheet/edit/EditSheetView'

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
    ReactDOM.render(<FolderTable folders={folders} onClick={f => showFolder(f.id)} />, document.getElementById("folders"));
}

function showFolder(id) {
    cleanSheet()
    fetchFromApi('/folder/' + id)
        .then(res => res.json().then(
            f => {
                fetchFromApi('/folder/' + id + '/sheets')
                    .then(res => res.json().then( sheets => {
                        ReactDOM.render(
                            fullSheetTable(f, sheets),
                            document.getElementById('sheets')
                        );
                    }))
            }))
}

function fullSheetTable(folder, sheets) {
    const sheetTable = (<SheetTable key={folder.id} folder={folder} sheets={sheets} onClick={ sheet => showSheet(sheet.id) } onEdit={ sheet => editSheet(folder, sheet, newSheet => showFolder(folder.id)) }/>)
    const addFolderButton = (<button onClick={() => editSheet(folder, null, newSheet => showFolder(folder.id))} >Add sheet</button>)

    return (
        <section>
            {addFolderButton}
            {sheetTable}
        </section>
    )
}

function editSheet(folder, sheet, callback) {
    const outerCallback = sheet === null ? newSheet => submitCreateSheet(newSheet, folder.id, callback) : updatedSheet => submitUpdateSheet(updatedSheet, callback)
    ReactDOM.render(
        <EditSheetView key={ sheet === null ? "newsheet" : sheet.id } folder={folder} sheet={sheet} onSubmit={outerCallback}/>,
        document.getElementById('sheet')
    )
    return 
}

function submitCreateSheet(sheet, folderId, callback) {
    postToApi('/sheet/' + `?folderId=${folderId}`, sheet)
        .then(res => res.json().then(sheet => callback(sheet)))
}

function submitUpdateSheet(sheet, callback) {
    putToApi('/sheet/' + `${sheet.id}`, sheet)
        .then(res => res.json().then(sheet => callback(sheet)))
}

function cleanSheet() {
    ReactDOM.render(<p></p>, document.getElementById('sheet'));
}

function showSheet(id) {
    fetchFromApi('/sheet/' + id)
        .then(res => res.json().then(
            s => {
                ReactDOM.render(
                    <PrettySheetView key={s.id} sheet={s} />,
                    document.getElementById('sheet')
                );
            }
        ))
}
