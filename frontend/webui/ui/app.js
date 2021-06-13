import Cookies from 'js-cookie'
import React, { useState } from 'react'
import ReactDOM from 'react-dom'

import { postToApi, putToApi } from './fetching'
import { FolderTable } from './folder/FolderTable'
import { SheetTable } from './sheet/SheetTable'
import { PrettySheetView } from './sheet/view/PrettySheetView'
import { EditSheetView } from './sheet/edit/EditSheetView'

const session = Cookies.get('session')
let user = Cookies.get('user')

function App() {
    const badSession = session === undefined || session === ""

    if (badSession) {
        return (
            <section>
                <h1>No session in cookies</h1>
            </section>
        );
    }

    const [lastTimeLoaded, setLastTimeLoaded] = useState(new Date())

    const [currentFolder, setCurrentFolder] = useState(null)
    const [currentSheet, setCurrentSheet] = useState(null)

    const [mode, setMode] = useState("viewing")

    const handleFolderClick = folder => {
        setCurrentFolder(folder)
        setCurrentSheet(null)
    }

    const handleSheetClick = sheet => {
        setCurrentSheet(sheet)
        setMode('viewing')
    }

    const handleSheetEdit = sheet => {
        setCurrentSheet(sheet)
        setMode('editing')
    }

    const afterSave = () => {
        setLastTimeLoaded(new Date())
        setCurrentSheet(null)
        setMode('viewing')
    }

    const sheetSection = !currentFolder ? (<></>) :
        (<SheetTable
            key={currentFolder.id + lastTimeLoaded}
            folder={currentFolder}
            onClick={handleSheetClick}
        />)

    return (
        <div>
            <FolderTable
                key={lastTimeLoaded}
                user={user}
                onClick={handleFolderClick}
            />
            { !currentFolder ? <></> :
                <section id="sheets">
                    <button onClick={() => handleSheetEdit({})} >Add sheet</button> :
                    {sheetSection}
                </section>
            }
            <section id="sheet">
                { !currentSheet ? <></> :
                    toSheetView(mode, currentSheet, currentFolder, handleSheetEdit, afterSave)
                }
            </section>
        </div>
    )
}

function toSheetView(mode, sheet, folder, handleEdit, afterSave) {
    switch (mode) {
        case 'viewing':
            return (
                <PrettySheetView
                    key={sheet.id}
                    sheet={sheet}
                    onEdit={handleEdit}
                />
            )
        case 'editing':
            const innerCallback = (sheet && sheet.id) ? updatedSheet => submitUpdateSheet(updatedSheet, afterSave) : newSheet => submitCreateSheet(newSheet, folder.id, afterSave);
            return (
                <EditSheetView
                    key={(sheet && sheet.id) ? sheet.id : "newsheet" + (new Date())}
                    folder={folder}
                    sheet={sheet}
                    onSubmit={innerCallback}
                />
            )
    }
}

function submitCreateSheet(sheet, folderId, callback) {
    postToApi('/sheet/' + `?folderId=${folderId}`, sheet)
        .then(res => res.json().then(sheet => callback(sheet)))
}

function submitUpdateSheet(sheet, callback) {
    putToApi('/sheet/' + `${sheet.id}`, sheet)
        .then(res => res.json().then(sheet => callback(sheet)))
}

ReactDOM.render(<App />, document.getElementById('app'));