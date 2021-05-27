import React, { useState } from 'react'
import { SheetRow } from './SheetRow'

/** requires 'sheets' array of sheets, 'folder' and an optional 'onClick' callback that will be passed on to SheetRow*/
export function SheetTable(props) {
    const folder = props.folder
    const [sheets, setSheets] = useState(props.sheets)
    const handleClick = props.onClick

    // const editable = props.editable
    const editable = true
    
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
                sheets.map(sheet => <SheetRow key={sheet.id} schema={schema} sheet={sheet} onClick={handleClick} onEdit={props.onEdit}/>)
            }
            </tbody>
        </table>
    )

}