import React, { useState } from 'react'

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