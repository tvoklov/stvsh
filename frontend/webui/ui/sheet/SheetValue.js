import React, { useState } from 'react'

export function renderableSheetValue(sheetId, name, value) {
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