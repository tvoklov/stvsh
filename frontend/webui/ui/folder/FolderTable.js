import React, { useState, useEffect } from 'react'
import { FolderRow } from './FolderRow'
import { fetchFromApi } from '../fetching'

export function FolderTable({ user, onClick }) {
    const [isLoaded, setIsLoaded] = useState(false)
    const [page, setPage] = useState(1)
    const [folders, setFolders] = useState([])

    useEffect(() => {
        fetchFromApi('/user/' + user + "/folders", {})
            .then(res => {
                res.json().then(fs => {
                    setFolders(fs)
                    setIsLoaded(true)
                })
            })
    }, [page])

    if (!isLoaded) {
        return (<p>Loading folders</p>)
    } else {
        return (
            <section>
                <table>
                    <thead>
                        <tr key="foldershead">
                            <th key="folderheader">Folder</th>
                        </tr>
                    </thead>
                    <tbody>
                        {
                            folders.map(f => <FolderRow key={f.id} folder={f} onClick={onClick} />)
                        }
                    </tbody>
                </table>
            </section>
        )
    }
}