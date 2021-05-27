import React, { useState } from 'react'

export function EditSheetView(props) {
    const folder = props.folder
    const [sheet, setSheet] = useState(props.sheet ? props.sheet : { values: {} })

    const f = ss => Object.entries(ss.values).reduce((map, obj) => ( map[obj[0]] = obj[1].value, map ), {})

    const [currentValues, setCurrentValues] = useState(props.sheet ? f(props.sheet) : [])

    const fields = Object.entries(folder.schema)

    const handleSubmit = () => {
        props.onSubmit({...sheet, values: Object.fromEntries(currentValues)})
    }

    const handleChange = name => ({target}) => {
        console.log(target.value)
        setCurrentValues(prev => {
            prev[name] = target.value
            console.log(prev)
            return prev
        })
    }

    return (
        <form>
            {
                fields.map(([name, type]) => {
                    const filledValue = currentValues[name]
                    if (filledValue) { // for now everything is just text
                        console.log(filledValue)
                        return (
                            <section key={name + filledValue.value}>
                                <label htmlFor={name}>{name}</label>
                                <input type="text" name={name} value={filledValue.value} onChange={handleChange(name)}/>
                            </section>
                        )
                    } else {
                        console.log(currentValues)
                        return (
                            <section key={name + 'null'}>
                                <label htmlFor={name}>{name}</label>
                                <input type="text" name={name} value="" onChange={handleChange(name)}/>
                            </section>
                        )
                    }
                })
            }
            <input type="button" value="Add sheet" onClick={handleSubmit}/>
        </form>
    )
}