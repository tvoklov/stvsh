import React, { useState } from 'react'
import { renderableSheetValue } from '../SheetValue'

export function BasicSheetView(props) {
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