package org.opentox.pol.httpreturn;

public class HttpReturn {
    public int status;
    public String data;
    public HttpReturn (int _s, String _d) {
        status = _s;
        data = _d;
    }
}
