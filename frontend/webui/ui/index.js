import Cookies from 'js-cookie'
import React from 'react'
import ReactDOM from 'react-dom'

const session = Cookies.get('session')
let user = Cookies.get('user')

if (session === undefined || session === "") {
    const badSession = (
        <section>
            <h1>No session in cookies</h1>
        </section>
    );
    ReactDOM.render(badSession, document.getElementById("folders"));
} else {
    fetch('/v1/user/' + user + "/folders", {
        headers: {
            session: session
        }
    }).then(res => {
        res.json().then(showFolders)
    })
}

function showFolders(folders) {
    const foldersHtml = (
        <section>
            <table>
                <thead>
                    <tr key="foldershead">
                        <th>Folder</th>
                    </tr>
                </thead>
                <tbody>
                    {
                        folders.map(folder => {
                            return (
                                <tr key={folder.id} onClick={() => showFolder(folder.id)}>
                                    <td>{folder.name}</td>
                                </tr>
                            )
                        })
                    }
                </tbody>
            </table>
        </section>
    )
    ReactDOM.render(foldersHtml, document.getElementById("folders"));
}

function showFolder(id) {
    fetch('/v1/folder/' + id, {
        headers: {
            session: session
        }
    })
        .then(res => res.json().then(
            f => {
                const schema = Object.entries(f.schema)
                
                fetch('/v1/folder/' + id + '/sheets', {
                    headers: {
                        session: session
                    }
                })
                    .then(res => res.json().then( sheets => {
                        const table =
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
                                        console.log(sheet)
                                        console.log(schema)
                                        return (
                                            <tr key={sheet.id}>
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

                        ReactDOM.render(table, document.getElementById("folder"));
                    }))

            }))
}
