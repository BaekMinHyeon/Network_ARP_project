import java.util.ArrayList;

public class IPLayer implements BaseLayer {
	
	public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    _IP m_sHeader;
    
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
    	byte[] ip_data; // variable length data
    	
    	public _IP(){
    		ip_verlen = 0x00; // git에는 0x45이유는 모르겠음
    		ip_tos = 0x00;
    		ip_len = new byte[2];
    		ip_id = new byte[2];
    		ip_fragoff = new byte[2];
    		ip_ttl = 0x00;
    		ip_proto = 0x00;
    		ip_cksum = new byte[2];
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
    
    public IPLayer(String pName){
    	pLayerName = pName;
    	m_sHeader = new _IP();
    }
    
	@Override
	public String GetLayerName() {
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
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
		this.p_UnderLayer = pUnderLayer;
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
