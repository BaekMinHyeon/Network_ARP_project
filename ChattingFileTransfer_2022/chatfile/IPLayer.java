import java.util.ArrayList;

public class IPLayer implements BaseLayer {
	public int nUnderLayerCount = 0;
	public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public ArrayList<BaseLayer> p_aUnderLayer = new ArrayList<BaseLayer>();
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    _IP m_sHeader;
    
    private int Header_Size = 20;
    private int Max_Data = 1480;
    private int offset = 185;
    private byte[] FragmentCollect;
    
    //----------생성자-------------
    public IPLayer(String pName){
    	pLayerName = pName;
    	m_sHeader = new _IP();
    }
    //------------frame-------------
    private class _IP {
    	byte ip_verlen; // 1byte
    	byte ip_tos; // 1byte
    	byte[] ip_len; // 2byte
    	byte[] ip_id; // 2byte
    	byte[] ip_fragoff; // 2byte
    	byte ip_ttl; // 1byte
    	byte ip_proto; // 1byte
    	byte[] ip_cksum; // 2byte
    	_IP_ADDR ip_src; // 4byte
    	_IP_ADDR ip_dst; // 4byte
    	// 여기까지 IP의 Header
    	byte[] ip_data; // variable length data
    	
    	public _IP(){
    		ip_verlen = 0x45;
    		ip_tos = 0x00; // not use
    		ip_len = new byte[2];
    		ip_id = new byte[2]; // not use
    		ip_fragoff = new byte[2];
    		ip_ttl = 0x00; // not use
    		ip_proto = 0x06;
    		ip_cksum = new byte[2]; // not use
    		ip_src = new _IP_ADDR();
    		ip_dst = new _IP_ADDR();
    		ip_data = null;
    	}
    }
    
    private class _IP_ADDR {
    	byte[] addr = new byte[4]; // ip_src, ip_dst
    	
    	public _IP_ADDR(){
    			this.addr[0] = (byte) 0x00;
    			this.addr[1] = (byte) 0x00;
    			this.addr[2] = (byte) 0x00;
    			this.addr[3] = (byte) 0x00;
    	}
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
    
    public void SetIPSrcAddress(byte[] srcAddress){
    	m_sHeader.ip_src.addr = srcAddress;
    }
    
    public void SetIPDstAddress(byte[] dstAddress){
    	m_sHeader.ip_dst.addr = dstAddress;
    }
    
    //-------------- BaseLayer 상속-------------------
	@Override
	public String GetLayerName() {
		return pLayerName;
	}
	
	@Override
	public BaseLayer GetUnderLayer(int nindex) {
		if (nindex < 0 || nindex > nUnderLayerCount || nUnderLayerCount < 0)
			return null;
		return p_aUnderLayer.get(nindex);
	}
	
	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		if (pUnderLayer == null)
			return;
		this.p_aUnderLayer.add(nUnderLayerCount++, pUnderLayer);
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
	
}
