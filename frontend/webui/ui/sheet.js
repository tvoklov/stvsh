import React, { useState } from 'react'

export function SheetRow(props) {
    const [sheet, setSheet] = useState(props.sheet)
    
    const handleClick = () => props.onClick(sheet)
    const schema = props.schema

    const smap = Object.entries(sheet.values)

    return (
        <tr key={sheet.id} className="clickable" onClick={handleClick}>
            {
                schema.map(([name, type]) => {
                    const elem = smap.find(([sname, stype]) => sname === name)
                    if (elem === undefined)
                        return (<td key={name}></td>)
                    else
                        return (<td key={name}>{elem[1].value}</td>)
                })
            }
        </tr>
    )

}

export function Sheet(props) {
    const [sheet, setSheet] = useState(props.sheet)
    
    return (
        <table>
            <tbody>
                {
                    Object.entries(sheet.values).map(
                        ([name, value]) => renderableSheetValue(sheet.id, name, value)
                    )
                }
            </tbody>
        </table>
    )

}

function renderableSheetValue(sheetId, name, value) {
    switch (value.type) {
        case 'text':
            return (<SheetText key={sheetId + name} name={name} value={value} />)
        case 'image':
            return (<SheetImage key={sheetId + name} name={name} value={value} />)
        default:
            return (<SheetText key={sheetId + name} name={name} value={value} />)
    }
}


function SheetText(props) {
    const [value, setValue] = useState(props.value)
    const name = props.name
    
    return (
        <tr>
            <td>{name}</td>
            <td>{value.value}</td>
        </tr>
    )
}

function SheetImage(props) {
    const [value, setValue] = useState(props.value)
    const name = props.name

    return (
        <tr>
            <td>{name}</td>
            <td><img src={value.value} alt={value.value}/></td>
        </tr>
    )
}

