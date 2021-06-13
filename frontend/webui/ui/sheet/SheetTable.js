import React, { useEffect, useState } from 'react'
import { SheetRow } from './SheetRow'
import { fetchFromApi } from '../fetching'

export function SheetTable({ folder, onClick }) {
    const [sheets, setSheets] = useState([])
    const [isLoaded, setIsLoaded] = useState(false)

    const [page, setPage] = useState(1)

    useEffect(() => {
        fetchFromApi('/folder/' + folder.id + '/sheets')
            .then(res => res.json().then(ss => {
                setSheets(ss)
                setIsLoaded(true)
            }))
    }, [page])

    const editable = true
    const schema = Object.entries(folder.schema)

    if (!isLoaded) {
        return (<p>Loading sheets...</p>)
    } else {
        return (
            <table>
                <thead>
                    <tr key='folderhead'>
                        {
                            schema.map(([name, type]) =>
                                <th key={name}>{name}</th>
                            )
                        }
                    </tr>
                </thead>
                <tbody>
                    {
                        sheets.map(sheet => <SheetRow key={sheet.id} schema={schema} sheet={sheet} onClick={onClick} />)
                    }
                </tbody>
            </table>
        )
    }
}