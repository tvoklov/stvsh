import React, { useState } from 'react'

/** @example <FolderRow folder={folder} onClick={function that accepts a folder} */
export function FolderRow({ folder, onClick }){
    const handleClick = () => onClick(folder)

    return (
        <tr key={folder.id} onClick={handleClick} className="clickable" >
            <td key="name">{folder.name}</td>
        </tr>
    )
}