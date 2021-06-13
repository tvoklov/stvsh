import React, { useState } from 'react'

export function SheetRow({sheet, schema, onClick}) {
    const handleClick = () => onClick(sheet)
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