public class MyAPIMtd {
    public String classname;
    public String classdes;
    public String mename;
    public String mename_old;
    public String medes;
    public String apisig;
    public String filename;
    public MyAPIMtd(String classname,String classdes,String mename,String medes,String apisig,String mename_Old,String filename) {

        this.classname = classname;
        this.classdes=classdes;
        this.medes = medes;
        this.mename=mename;
        this.apisig=apisig;
        this.filename=filename;
        this.mename_old=mename_Old;
    }
    @Override
    public String toString() {
        StringBuffer sbr = new StringBuffer();
        sbr.append(classname);
        sbr.append("\n");
        sbr.append(mename);
        sbr.append("\n-----------------------\n");
        return sbr.toString();
    }
}
