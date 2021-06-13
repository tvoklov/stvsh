import React, { useState } from 'react'
import { renderableSheetValue } from '../SheetValue'

export function BasicSheetView({sheet}) {
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