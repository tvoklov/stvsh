import React, { useState } from 'react'
import { renderableSheetValue } from '../SheetValue'

export function PrettySheetView(props) {
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