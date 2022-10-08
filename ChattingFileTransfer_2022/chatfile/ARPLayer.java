import java.util.ArrayList;

public class ARPLayer implements BaseLayer {

    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    public ArrayList<_ARP_ARR> proxyTable = new ArrayList<_ARP_ARR>();
    public ArrayList<_ARP_ARR> arpTable = new ArrayList<_ARP_ARR>();
    _ARP_Frame m_sHeader;

    public ARPLayer(String pName) {
        // super(pName);
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        ResetHeader();
    }

    public void ResetHeader() {
        m_sHeader = new _ARP_Frame();
    }

    public byte[] ObjToByte(_ARP_Frame Header, byte[] input, int length) {//data에 헤더 붙여주기
        byte[] buf = new byte[length + 28];
        for(int i = 0; i < 2; i++) {
            buf[i] = Header.hard_type[i];
            buf[i+2] = Header.prot_type[i];
        }

        buf[4] = Header.hard_size[0];
        buf[5] = Header.prot_size[0];
        buf[6] = Header.opcode[0];
        buf[7] = Header.opcode[1];

        for(int i = 0; i < 6; i++) {
            buf[i+8] = Header.enet_sender_addr.addr[i];
            buf[i+18] = Header.enet_target_addr.addr[i];
        }
        for(int i = 0; i < 4; i++) {
            buf[i+14] = Header.ip_sender_addr.addr[i];
            buf[i+24] = Header.ip_target_addr.addr[i];
        }
        for (int i = 0; i < length; i++)
            buf[14 + i] = input[i];

        return buf;
    }

    public byte[] ProxyObjToByte(_ARP_Frame Header, byte[] input, int length) {//data에 헤더 붙여주
        byte[] temp = new byte[1];

        for(int i = 0; i < 6; i++) {
            temp[0] = input[i+8];
            input[i+8] = input[i+18];
            input[i+18] = temp[0];
        }

        for(int i = 0; i < 4; i++) {
            temp[0] = input[i+14];
            input[i+14] = input[i+24];
            input[i+24] = temp[0];
        }

        return input;
    }

    public boolean Send(byte[] input, int length) {
        m_sHeader.opcode = intToByte2(1);

        byte[] bytes = ObjToByte(m_sHeader, input, length);

        this.GetUnderLayer().SendArp(bytes, bytes.length);
        return true;
    }

    public boolean ReturnSend(byte[] input, int length) {
        m_sHeader.opcode = intToByte2(2);

        byte[] bytes = ObjToByte(m_sHeader, input, length);

        this.GetUnderLayer().SendArp(bytes, bytes.length);
        return true;
    }


    public boolean ProxySend(byte[] input, int length) {

        m_sHeader.opcode = intToByte2(2);


        byte[] bytes = ProxyObjToByte(m_sHeader, input, length);

        this.GetUnderLayer().SendArp(bytes, bytes.length);

        return true;
    }

    public byte[] RemoveArpHeader(byte[] input, int length) {
        byte[] cpyInput = new byte[length - 28];
        System.arraycopy(input, 28, cpyInput, 0, length - 28);
        input = cpyInput;
        return input;
    }

    public void DestinationSet(byte[] input) {
        m_sHeader.enet_target_addr.addr[0] = input[8];
        m_sHeader.enet_target_addr.addr[1] = input[9];
        m_sHeader.enet_target_addr.addr[2] = input[10];
        m_sHeader.enet_target_addr.addr[3] = input[11];
        m_sHeader.enet_target_addr.addr[4] = input[12];
        m_sHeader.enet_target_addr.addr[5] = input[13];
        m_sHeader.ip_target_addr.addr[0] = input[14];
        m_sHeader.ip_target_addr.addr[1] = input[15];
        m_sHeader.ip_target_addr.addr[2] = input[16];
        m_sHeader.ip_target_addr.addr[3] = input[17];
    }

    public boolean ArpTableSet() {
        for(_ARP_ARR addr : arpTable){
            if(addr.ip_target_addr.addr == m_sHeader.ip_target_addr.addr) {
                addr.enet_target_addr.addr = m_sHeader.enet_target_addr.addr;
                return true;
            }
        }
        arpTable.add(new _ARP_ARR(m_sHeader.enet_target_addr.addr, m_sHeader.ip_target_addr.addr));
        return true;
    }

    public boolean ProxyTableSet(byte[] enthernet_addr, byte[] ip_addr) {
        proxyTable.add(new _ARP_ARR(enthernet_addr, ip_addr));
        return true;
    }
    //
    public synchronized boolean Receive(byte[] input) {
        byte[] data;
        int temp_type = byte2ToInt(input[6], input[7]);
        if (temp_type == 1) {
            DestinationSet(input);
            ArpTableSet();
            if(!isMyPacket(input) && chkAddr(input)){
                this.ReturnSend(null, 0);
                //화면 출력
                return true;
            }
            else{
                //프록시 테이블 확인
                for (_ARP_ARR addr : proxyTable){
                    if (chkProxyAddr(addr.ip_target_addr.addr, input)) {
                        this.ProxySend(input, input.length);
                    }
                }
                //화면 출력(테이블 저장)
            }
        }
        else if (temp_type == 2) {
            DestinationSet(input);
            ArpTableSet();
            //테이블 출력
            return true;
        }
        return false;
    }

    private byte[] intToByte2(int value) {
        byte[] temp = new byte[2];
        temp[0] |= (byte) ((value & 0xFF00) >> 8);
        temp[1] |= (byte) (value & 0xFF);

        return temp;
    }

    private int byte2ToInt(byte value1, byte value2) {
        return (int)(((value1 & 0xff) << 8) | (value2 & 0xff));
    }
    //
    private boolean isBroadcast(byte[] bytes) {
        for(int i = 0; i< 6; i++)
            if (bytes[i] != (byte) 0xff)
                return false;
        return (bytes[12] == (byte) 0xff && bytes[13] == (byte) 0xff);
    }

    private boolean isMyPacket(byte[] input){
        for(int i = 0; i < 4; i++)
            if(m_sHeader.ip_sender_addr.addr[i] != input[14 + i])
                return false;
        return true;
    }

    private boolean chkAddr(byte[] input) {
        for(int i = 0; i< 4; i++)
            if(m_sHeader.ip_sender_addr.addr[i] != input[24 + i])
                return false;
        return true;
    }

    private boolean chkProxyAddr(byte[] addr, byte[] input) {
        for(int i = 0; i< 4; i++)
            if(addr[i] != input[24 + i])
                return false;
        return true;
    }

    public void SetEnetSrcAddress(byte[] srcAddress) {
        // TODO Auto-generated method stub
        m_sHeader.enet_sender_addr.addr = srcAddress;
    }

    public void SetEnetDstAddress(byte[] dstAddress) {
        // TODO Auto-generated method stub
        m_sHeader.enet_target_addr.addr = dstAddress;
    }

    public void SetIpSrcAddress(byte[] srcAddress) {
        // TODO Auto-generated method stub
        m_sHeader.ip_sender_addr.addr = srcAddress;
    }

    public void SetIpDstAddress(byte[] dstAddress) {
        // TODO Auto-generated method stub
        m_sHeader.ip_target_addr.addr = dstAddress;
    }

    @Override
    public String GetLayerName() {
        // TODO Auto-generated method stub
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        // TODO Auto-generated method stub
        if (p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        // TODO Auto-generated method stub
        if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        // TODO Auto-generated method stub
        if (pUnderLayer == null)
            return;
        this.p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        // TODO Auto-generated method stub
        if (pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }

    private class _ARP_ENTHERNET_ADDR {
        private byte[] addr = new byte[6];

        public _ARP_ENTHERNET_ADDR() {
            this.addr[0] = 0;
            this.addr[1] = 0;
            this.addr[2] = 0;
            this.addr[3] = 0;
            this.addr[4] = 0;
            this.addr[5] = 0;
        }
        public _ARP_ENTHERNET_ADDR(byte[] tempaddr) {
            this.addr[0] = tempaddr[0];
            this.addr[1] = tempaddr[1];
            this.addr[2] = tempaddr[2];
            this.addr[3] = tempaddr[3];
            this.addr[4] = tempaddr[4];
            this.addr[5] = tempaddr[5];
        }
    }

    private class _ARP_IP_ADDR {
        private byte[] addr = new byte[4];

        public _ARP_IP_ADDR() {
            this.addr[0] = 0;
            this.addr[1] = 0;
            this.addr[2] = 0;
            this.addr[3] = 0;
        }

        public _ARP_IP_ADDR(byte[] tempaddr) {
            this.addr[0] = tempaddr[0];
            this.addr[1] = tempaddr[1];
            this.addr[2] = tempaddr[2];
            this.addr[3] = tempaddr[3];
        }
    }


    private class _ARP_Frame {
        byte[] hard_type = new byte[2];
        byte[] prot_type = new byte[2];

        byte[] hard_size = new byte[1];
        byte[] prot_size = new byte[1];

        byte[] opcode = new byte[2];

        _ARP_ENTHERNET_ADDR enet_sender_addr = ARPLayer.this.new _ARP_ENTHERNET_ADDR();
        _ARP_IP_ADDR ip_sender_addr = ARPLayer.this.new _ARP_IP_ADDR();

        _ARP_ENTHERNET_ADDR enet_target_addr = ARPLayer.this.new _ARP_ENTHERNET_ADDR();
        _ARP_IP_ADDR ip_target_addr = ARPLayer.this.new _ARP_IP_ADDR();

        public _ARP_Frame() {
            hard_type[0] = 0x01;
            hard_type[1] = 0x00;
            prot_type[0] = 0x00;
            prot_type[1] = 0x08;
            hard_size[0] = 6;
            prot_size[0] = 4;
        }
    }

    private class _ARP_ARR {

        _ARP_ENTHERNET_ADDR enet_target_addr;
        _ARP_IP_ADDR ip_target_addr;

        public _ARP_ARR(byte[] enthernet_addr, byte[] ip_addr) {
            enet_target_addr = new _ARP_ENTHERNET_ADDR(enthernet_addr);
            ip_target_addr = new _ARP_IP_ADDR(ip_addr);
        }
    }

}
