import React, { useState } from 'react'
import { FolderRow } from './FolderRow'

/** requires 'folders' array of folders and an optional 'onClick' callback, that will be passed on to FolderRow */
export function FolderTable(props) {
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