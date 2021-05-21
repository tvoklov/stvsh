import React, { useState } from 'react'
import { SheetRow } from './sheet'

/** requires 'sheets' array of sheets, 'folder' and an optional 'onClick' callback that will be passed on to SheetRow*/
export function SheetTable(props) {
    const folder = props.folder
    const [sheets, setSheets] = useState(props.sheets)
    const handleClick = props.onClick
    
    const schema = Object.entries(folder.schema)

    return (
        <table>
            <thead>
                <tr key='folderhead'>
                {
                    schema.map(([name, type]) =>
                        <th key={name}>{name}</th>
                    )
                }
                </tr>
            </thead>
            <tbody>
            {
                sheets.map(sheet => <SheetRow key={sheet.id} schema={schema} sheet={sheet} onClick={handleClick} />)
            }
            </tbody>
        </table>
    )

}

/** requires 'folders' array of folders and an optional 'onClick' callback, that will be passed on to FolderRow */
export function FoldersTable(props) {
    const [folders, setFolders] = useState(props.folders)
    const handleClick = props.onClick

    return (
        <section>
            <table>
                <thead>
                    <tr key="foldershead">
                        <th key="folderheader">Folder</th>
                    </tr>
                </thead>
                <tbody>
                    {
                        folders.map(f => <FolderRow key={f.id} folder={f} onClick={handleClick} />)
                    }
                </tbody>
            </table>
        </section>
    )
}

/** @example <FolderRow folder={folder} onClick={function that accepts a folder} */
export function  FolderRow(props){
    const [folder, setFolder] = useState(props.folder)
    const handleClick = () => props.onClick(folder)

    return (
        <tr key={folder.id} onClick={handleClick} className="clickable" >
            <td key="name">{folder.name}</td>
        </tr>
    )
}