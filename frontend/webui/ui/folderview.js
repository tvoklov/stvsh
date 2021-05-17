import React from 'react'

export function sheetTable(folder, sheets, sheetOnClick) {
    const schema = Object.entries(folder.schema)

    return (
        <table>
            <thead>
                <tr key='folderhead'>
                {
                    schema.map(([name, type]) => {
                        return (<th>{name}</th>)
                    })
                }
                </tr>
            </thead>
            <tbody>
            {
                sheets.map(sheet => {
                    const smap = Object.entries(sheet.values)
                    return (
                        <tr key={sheet.id} onClick={() => sheetOnClick(sheet)}>
                            {
                                schema.map(([name, type]) => {
                                    const elem = smap.find(([sname, stype]) => sname === name)
                                    if (elem === undefined)
                                        return (<td></td>)
                                    else
                                        return (<td>{elem[1].value}</td>)
                                })
                            }
                        </tr>
                    )
                })
            }
            </tbody>
        </table>
    )

}

export function foldersTable(folders, onClick) {
    return (
        <section>
            <table>
                <thead>
                    <tr key="foldershead">
                        <th>Folder</th>
                    </tr>
                </thead>
                <tbody>
                    {
                        folders.map(f => folderRow(f, onClick))
                    }
                </tbody>
            </table>
        </section>
    )
}

function folderRow(folder, onClick) {
    return (
        <tr key={folder.id} onClick={() => onClick(folder)}>
            <td>{folder.name}</td>
        </tr>
    )
}