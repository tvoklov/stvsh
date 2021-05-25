import React, { useState } from 'react'

export function EditSheetView(props) {
    const folder = props.folder
    const [sheet, setSheet] = useState(props.sheet ? props.sheet : { values: {} })
    const [currentValues, setCurrentValues] = useState(props.sheet ? f(props.sheet) : [])

    const f = ss =>
        Object.entries(ss.values).reduce((map, obj) => ( map[obj[0]] = obj[1].value, map ), {})

    const fields = Object.entries(folder.schema)
    const values = Object.entries(sheet.values)

    const handleSubmit = () => props.onSubmit(sheet)

    const handleChange = name => ({target}) => setCurrentValues(prev => (prev[name] = target.value, prev))

    const realValues =
        fields.map(([name, type]) => {
            const filledValue = values.find(([vname, value]) => {
                name === vname
            })
            if (filledValue) { // for now everything is just text
                return (
                    <section key={name}>
                        <label htmlFor={name}>{name}</label>
                        <input type="text" name={name} value={filledValue[1].value} onChange={handleChange}></input>
                    </section>
                )
            } else {
                return (
                    <section key={name}>
                        <label htmlFor={name}>{name}</label>
                        <input type="text" name={name} value="" onChange={handleChange}></input>
                    </section>
                )
            }
        })

    return (
        <form onSubmit={handleSubmit}>
            {realValues}
        </form>
    )
}