import React, { useEffect, useState } from "react";
import { makeStyles } from '@material-ui/core/styles';
import Box from '@material-ui/core/Box';
import Collapse from '@material-ui/core/Collapse';
import IconButton from '@material-ui/core/IconButton';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Typography from '@material-ui/core/Typography';
import Paper from '@material-ui/core/Paper';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@material-ui/icons/KeyboardArrowUp';
import CircularProgress from '@material-ui/core/CircularProgress';
import CloudDoneIcon from '@material-ui/icons/CloudDone';
import HelpIcon from '@material-ui/icons/Help';
import './Dashboard.css';

const useRowStyles = makeStyles({
    root: {
        '& > *': {
            borderBottom: 'unset',
        },
    },
});

function Dashboard(props) {
    console.log(props);
    const [services, setServices] = useState();
    const [error, setError] = useState();
    const [loading, setLoading] = useState(true);

    const url = '/services';
    const headers = {};

    useEffect(() => {
        const abortController = new AbortController();
        const fetchData = async () => {
            setLoading(true);
            try {
                const response = await fetch(url, { headers, signal: abortController.signal });
                if (!response.ok) {
                    const data = await response.json();
                    console.log(data);
                    setError(data);
                    setLoading(false);
                } else {
                    const data = await response.json();
                    console.log(data);
                    setServices(data);
                    setLoading(false);
                }
            } catch (e) {
                if (!abortController.signal.aborted) {
                    console.log(e);
                    setLoading(false);
                }
            }
        };

        fetchData();

        return () => {
            abortController.abort();
        };
    }, []);

    let wait;
    console.log(loading, error, services);

    if (loading) {
        console.log('display circular progress');
        wait = (<div><CircularProgress /></div>);
    } else if (error) {
        console.log('display error');
        wait = (
            <div>
                <pre>{JSON.stringify(error, null, 2)}</pre>
            </div>
        )
    } else if (services) {
        console.log('display services = ', services);
        wait = (
            <TableContainer component={Paper}>
                <Table aria-label="collapsible table">
                    <TableHead>
                        <TableRow>
                            <TableCell />
                            <TableCell>Service Id</TableCell>
                            <TableCell>Environment Tag</TableCell>
                            <TableCell align="right">Number of Nodes</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {Object.keys(services).map((id, i) => (
                            <Row key={i} history={props.history} id={id} nodes={services[id]} />
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        )
    }
    return (
        <div className="App">
            {wait}
        </div>
    );
}

function Row(props) {
    const { id, nodes, history } = props;
    const [open, setOpen] = React.useState(false);
    const classes = useRowStyles();
    const words = id.split('|');
    const serviceId = words[0];
    const tag = words[1];

    const handleCheck = (node) => {
        const k = id + ':' + node.address + ':' + node.port;
        console.log(node, k);
        history.push({ pathname: '/check', state: { data: { id: k } } });
    }

    const handleInfo = (node) => {
        const k = node.address + ':' + node.port;
        console.log(node, k);
        history.push({ pathname: '/info', state: { data: { node: k } } });
    }

    return (
        <React.Fragment>
            <TableRow className={classes.root}>
                <TableCell>
                    <IconButton aria-label="expand row" size="small" onClick={() => setOpen(!open)}>
                        {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
                    </IconButton>
                </TableCell>
                <TableCell component="th" scope="row">
                    {serviceId}
                </TableCell>
                <TableCell>{tag}</TableCell>
                <TableCell align="right">{nodes.length}</TableCell>
            </TableRow>
            <TableRow>
                <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
                    <Collapse in={open} timeout="auto" unmountOnExit>
                        <Box margin={1}>
                            <Typography variant="h6" gutterBottom component="div">
                                Nodes
              </Typography>
                            <Table size="small" aria-label="purchases">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Address</TableCell>
                                        <TableCell align="right">Port</TableCell>
                                        <TableCell align="right">Status Check</TableCell>
                                        <TableCell align="right">Server Info</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {nodes.map((node, j) => (
                                        <TableRow key={j}>
                                            <TableCell component="th" scope="row">
                                                {node.address}
                                            </TableCell>
                                            <TableCell align="right">{node.port}</TableCell>
                                            <TableCell align="right"><CloudDoneIcon onClick={() => handleCheck(node)} /></TableCell>
                                            <TableCell align="right"><HelpIcon onClick={() => handleInfo(node)} /></TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </Box>
                    </Collapse>
                </TableCell>
            </TableRow>
        </React.Fragment>
    );
}

export default Dashboard;
