import React, { useState, useEffect } from 'react'

export function EditSheetView({ folder, sheet, onSubmit }) {
    const f = ss => new Map(Object.entries(ss.values).map(([key, {value}]) => [key, value]))

    const [currentValues, setCurrentValues] = useState(sheet ? f(sheet) : new Map([]));

    const fields = Object.entries(folder.schema)

    const handleSubmit = () => {
        const values = Object.fromEntries(currentValues)
        onSubmit({...sheet, values: values})
    }

    const handleChange = name => ({target}) => {
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
                fields.map(([name, type]) => {
                    const filledValue = currentValues.get[name]
                    if (filledValue !== undefined) { // for now everything is just text
                        console.log('rendering' + filledValue)
                        return (
                            <section key={name + filledValue.value}>
                                <label htmlFor={name}>{name}</label>
                                <input type="text" name={name} value={filledValue.value} onChange={handleChange(name)}/>
                            </section>
                        )
                    } else {
                        console.log('rendering empty')
                        return (
                            <section key={name + 'null'}>
                                <label htmlFor={name}>{name}</label>
                                <input type="text" name={name} value={""} onChange={handleChange(name)}/>
                            </section>
                        )
                    }
                })
            }
            <input type="button" value="Add sheet" onClick={handleSubmit}/>
        </form>
    )
}