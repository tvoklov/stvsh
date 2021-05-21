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

export function BasicSheet(props) {
    const [sheet, setSheet] = useState(props.sheet)
    const values = sheet.values

    return (
        <table>
            <tbody>
                {
                    Object.entries(values).map(
                        ([name, value]) => renderableSheetValue(sheet.id, name, value)
                    )
                }
            </tbody>
        </table>
    )
}

export function PrettySheet(props) {
    const [sheet, setSheet] = useState(props.sheet)
    const values = sheet.values
    const entries = Object.entries(values)

    const img = entries.find(([name, value]) => value.type === 'image')

    const imgTag =
        img ? (
            <figure>
                <img src={img[1].value} alt={img[1].value}/>
                <figcaption>Image in {img[0]}</figcaption>
            </figure>) : <></>

    const entriesTag =
        img ?
            entries.filter(([name, ]) => name !== img[0]).map(
                ([name, value]) => renderableSheetValue(sheet.id, name, value)
            ) :
            entries.map(
                ([name, value]) => renderableSheetValue(sheet.id, name, value)
            )

    return (
        <div>
            {imgTag}
            <table>
                <tbody>
                    {entriesTag}
                </tbody>
            </table>
        </div>
    )
}

function renderableSheetValue(sheetId, name, value) {
    switch (value.type) {
        case 'text':
            return (<TextRow key={name + sheetId} name={name} value={value} />)
        case 'image':
            return (<ImageRow key={name + sheetId} name={name} value={value} />)
        default:
            return (<TextRow key={name + sheetId} name={name} value={value} />)
    }
}


function TextRow(props) {
    const [value, setValue] = useState(props.value)
    const name = props.name
    
    return (
        <tr>
            <td>{name}</td>
            <td>{value.value}</td>
        </tr>
    )
}

function ImageRow(props) {
    const [value, setValue] = useState(props.value)
    const name = props.name

    return (
        <tr>
            <td>{name}</td>
            <td><img src={value.value} alt={value.value}/></td>
        </tr>
    )
}

