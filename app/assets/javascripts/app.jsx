var ProcedureDetail = React.createClass({
    loadProcedures: function() {
        $.ajax({
            url: this.props.url,
            dataType: 'json',
            cache: false,
            success: function(data) {
                this.setState({data: data});
            }.bind(this),
            error: function(xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function() {
        return {data: []};
    },
    componentDidMount: function() {
        this.loadProcedures();
        setInterval(this.loadProcedures, this.props.pollInterval);
    },
    render: function() {
        return (
            <div className="">
                <h1>Procedures</h1>
                <ProcedureForm />
                <Procedures data={this.state.data} />
            </div>
        );
    }
});

var Procedures = React.createClass({
    render: function() {
        var procedureNodes = this.props.data.map(function (procedure) {
            return (
                <Procedure key={procedure.id} name={procedure.name} address={procedure.address} s3url={procedure.s3url} />
            );
        });

        return (
            <div className="well">
                {procedureNodes}
            </div>
        );
    }
});

var Procedure = React.createClass({
    render: function() {
        return (
            <blockquote>
                <p>{this.props.name}</p>
                <strong>{this.props.address}</strong>
                <small>{this.props.s3url}</small>
            </blockquote>
        );
    }
});

var ProcedureForm = React.createClass({
    handleSubmit: function(e) {
        e.preventDefault();

        var formData = $("#procedureForm").serialize();

        var saveUrl = "http://localhost:9000/procedures/save";
        $.ajax({
            url: saveUrl,
            method: 'POST',
            dataType: 'json',
            data: formData,
            cache: false,
            success: function(data) {
                console.log(data)
            }.bind(this),
            error: function(xhr, status, err) {
                console.error(saveUrl, status, err.toString());
            }.bind(this)
        });

        // clears the form fields
        React.findDOMNode(this.refs.name).value = '';
        React.findDOMNode(this.refs.address).value = '';
        React.findDOMNode(this.refs.s3url).value = '';
        return;
    },
    render: function() {
        return (
            <div className="row">
                <form id="procedureForm" onSubmit={this.handleSubmit}>
                    <div className="col-xs-3">
                        <div className="form-group">
                            <input type="text" name="name" required="required" ref="name" placeholder="Name" className="form-control" />
                        </div>
                    </div>
                    <div className="col-xs-3">
                        <div className="form-group">
                            <input type="text" name="address"required="required"  ref="address" placeholder="Address" className="form-control" />
                        </div>
                    </div>
                    <div className="col-xs-3">
                        <div className="form-group">
                            <input type="text" name="s3url" required="required" ref="s3url" placeholder="S3url" className="form-control" />
                            <span className="input-icon fui-check-inverted"></span>
                        </div>
                    </div>
                    <div className="col-xs-3">
                        <input type="submit" className="btn btn-block btn-info" value="Add" />
                    </div>
                </form>
            </div>
        );
    }
});

React.render(<ProcedureDetail url="http://localhost:9000/procedures" pollInterval={2000} />, document.getElementById('content'));