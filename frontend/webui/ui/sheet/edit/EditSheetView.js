import React, { useState, useEffect } from 'react'

function sheetValuesToMap(values) {
    return new Map(Object.entries(values).map(([key, {value}]) => [key, value]))
}

export function EditSheetView({ folder, sheet, onSubmit }) {
    const [currentValues, setCurrentValues] =
        useState((sheet && sheet.id && sheet.values) ?
            sheetValuesToMap(sheet.values) :
            new Map([])
        )
    
    const editMode = sheet ? sheet.id : false
    const fields = Object.entries(folder.schema)

    const handleSubmit = () => {
        const values = Object.fromEntries(currentValues)
        onSubmit({...sheet, values: values})
    }

    const handleChangeValue = name => ({target}) => {
        setCurrentValues(prev =>
            new Map([
                ...prev,
                [name, target.value]
            ])
        );
    }

    return (
        <form>
            {
                fields.map(([name, type], index) => {
                    const filledValue = currentValues.get(name)
                    return (
                        <section key={index + name}>
                            <label htmlFor={name}>{name}</label>
                            <input type="text" name={name} value={filledValue ? filledValue : ""} onChange={handleChangeValue(name)}/>
                        </section>
                    )
                })
            }
            <input type="button" value={editMode ? "Save sheet" : "Add sheet"} onClick={handleSubmit}/>
        </form>
    )
}