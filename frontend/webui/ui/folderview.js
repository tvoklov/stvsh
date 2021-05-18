import React from 'react'
import { SheetRow } from './sheet'

export function sheetTable(folder, sheets, sheetOnClick) {
    const schema = Object.entries(folder.schema)

    return (
        <table>
            <thead>
                <tr key='folderhead'>
                {
                    schema.map(([name, type]) => {
                        return (<th key={name}>{name}</th>)
                    })
                }
                </tr>
            </thead>
            <tbody>
            {
                sheets.map(sheet => <SheetRow key={sheet.id} schema={schema} sheet={sheet} onClick={sheetOnClick} />)
            }
            </tbody>
        </table>
    )

}

/** @example <FoldersTable folders={ArrayOfFolders} onClick={function of what to do when a folder is clicked on} */
export class FoldersTable extends React.Component {
    constructor (props) {
        super(props)
        this.state = {
            folders: props.folders
        }
    }

    render() {
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
                            this.state.folders.map(f => <FolderRow key={f.id} folder={f} onClick={this.props.onClick} />)
                        }
                    </tbody>
                </table>
            </section>
        )
    }
}

/** @example <FolderRow folder={folder} onClick={function that accepts a folder} */
export class FolderRow extends React.Component{
    constructor (props) {
        super(props)
        this.state = {
            folder: props.folder
        }
        this.onClick = this.onClick.bind(this)
    }

    onClick() {
        this.props.onClick(this.state.folder)
    }

    render() {
        const f = this.state.folder
        return (
            <tr key={f.id} onClick={this.onClick} className="clickable" >
                <td key="name">{f.name}</td>
            </tr>
        )
    }
}