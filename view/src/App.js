import React, { useEffect, useState } from "react";
import { HashRouter, Route, Switch, Redirect } from "react-router-dom";
import Dashboard from "./Dashboard";
import HealthCheck from "./HealthCheck";
import ServerInfo from "./ServerInfo";

const App = () => {
  return (
    <HashRouter>
      <Switch>
        <Route exact path="/" component={Dashboard} />
        <Route path="/check" component={HealthCheck} />
        <Route path="/info" component={ServerInfo} />
      </Switch>
    </HashRouter>
  );
}

export default App;
