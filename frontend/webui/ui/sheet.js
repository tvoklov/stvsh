import React from 'react'

export class SheetRow extends React.Component {
    constructor (props) {
        super(props)
        this.state = {
            sheet: props.sheet
        }
        
        this.handleClick = this.handleClick.bind(this)
    }

    handleClick() {
        this.props.onClick(this.state.sheet)
    }

    render() {
        const smap = Object.entries(this.state.sheet.values)
        return (
            <tr key={this.state.sheet.id} className="clickable" onClick={this.handleClick}>
                {
                    this.props.schema.map(([name, type]) => {
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

}

export class Sheet extends React.Component {
    constructor (props) {
        super(props)
        this.state = {
            id: props.sheet.id,
            sheet: props.sheet
        }
    }

    render() {
        return (
            <table>
                <tbody>
                    {
                        Object.entries(this.state.sheet.values).map(
                            ([name, value]) => renderableSheetValue(name, value)
                        )
                    }
                </tbody>
            </table>
        )
    }

}

function renderableSheetValue(name, value) {
    console.log(value)
    console.log(name)
    switch(value.type) {
        case 'text':
            return (<SheetText key={name} name={name} value={value} />)
        case 'image':
            return (<SheetImage key={name} name={name} value={value} />)
        // case 'tags':
        //     return (<SheetTags name={name} value={value} />)
        default:
            return (<SheetText key={name} name={name} value={value} />)
    }
}


class SheetText extends React.Component {
    constructor (props) {
        super(props)
        this.state = {
            name: props.name,
            value: props.value.value
        }
    }

    render() {
        return (
            <tr key={this.state.name}>
                <td key="name">{this.state.name}</td>
                <td key="value">{this.state.value}</td>
            </tr>
        )
    }
}

class SheetImage extends React.Component {
    constructor (props) {
        super(props)
        this.state = {
            name: props.name,
            value: props.value.value
        }
    }

    render() {
        return (
            <tr key={this.state.name}>
                <td key="name">{this.state.name}</td>
                <td key="value"><img src={this.state.value} alt={this.state.value}/></td>
            </tr>
        )
    }
}

