import java.util.ArrayList;

import EthernetLayer._ETHERNET_Frame;

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
    
    public boolean Send(byte[] input, int length){
    	int fragment = input[13] & 0xff; // TCP의 flag를 확인
    	int sequence = byte4ToInt(input[4], input[5], input[6], input[7]); // TCP의 seq을 확인
    	if(fragment == 0){ // 단편화 없음
    		setHeader(length, 0, 0, sequence * offset);
    	}
    	else{ // 단편화
    		if(fragment == 1) // 처음~마지막 전 데이터
    			setHeader(length, 1, 1, sequence * offset);
    		else // fragment == 2 마지막 데이터
    			setHeader(length, 1, 1, sequence * offset);
    	}
    	byte[] data = ObjToByte(m_sHeader, input, length);
    	EthernetLayer ethernetLayer = (EthernetLayer)this.GetUnderLayer(1); // 0일지 1일지 Dlg를 만들지 않아 미정
    	ethernetLayer.Send(data, data.length);
    	return true;
    }
    
    public boolean SendARP(byte[] dstAddr){
    	ARPLayer arpLayer = (ARPLayer)this.GetUnderLayer(0); // 0일지 1일지 Dlg를 만들지 않아 미정
    	arpLayer.; // 메소드 이름 몰라서 아직 안씀
    	return true;
    }
    
    public boolean SendGARP(byte[] srcMac){
    	ARPLayer arpLayer = (ARPLayer)this.GetUnderLayer(0); // 0일지 1일지 Dㅣg를 만들지 않아 미정
    	arpLayer.; // 메소드 이름 몰라서 아직 안씀
    	return true;
    }
    
    private void setHeader(int length, int frag, int more, int fragoff){
    	this.m_sHeader.ip_len = intToByte2(length); // tcp에서 전송되는 데이터 최대 1480이라서 2바이트 변환이면 충분
    	// frag: 0(단편화 없음), 1(단편화 됨)  more: 0(마지막 데이터), 1(아직 데이터 남음)
    	// ip_fragoff 16비트 중 맨 앞은 0 1비트 frag 1비트 more 13비트 fragoff
    	this.m_sHeader.ip_fragoff[0] = (byte)(((frag & 0x1) << 6) | ((more & 0x1) << 5) | ((fragoff & 0x1f00) >> 8));
    	this.m_sHeader.ip_fragoff[1] = (byte)(fragoff & 0xff);  	
    }
    
    private byte[] ObjToByte(_IP Header, byte[] input, int length) {//data에 헤더 붙여주기
		byte[] buf = new byte[length + Header_Size];
		buf[0] = Header.ip_verlen;
		buf[1] = Header.ip_tos;
		for(int i = 0; i < 2; i++){
			buf[2 + i] = Header.ip_len[i];
			buf[4 + i] = Header.ip_id[i];
			buf[6 + i] = Header.ip_fragoff[i];
			buf[10 + i] = Header.ip_cksum[i];
		}
		buf[8] = Header.ip_ttl;
		buf[9] = Header.ip_proto;
		for(int i = 0; i < 4; i++){
			buf[12 + i] = Header.ip_src.addr[i];
			buf[16 + i] = Header.ip_dst.addr[i];
		}
		for (int i = 0; i < length; i++)
			buf[Header_Size + i] = input[i];
		return buf;
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
    
    private int byte4ToInt(byte value1, byte value2, byte value3, byte value4){
    	return (int)(((value1 & 0xff) << 24) | ((value2 & 0xff) << 16) | ((value3 & 0xff) << 8) | (value4 & 0xff));
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
