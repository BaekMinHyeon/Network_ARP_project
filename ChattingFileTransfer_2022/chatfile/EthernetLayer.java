import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {
	public int nUnderLayerCount = 0;
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	ArrayList<BaseLayer> p_aUnderLayer = new ArrayList<BaseLayer>();
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	_ETHERNET_Frame m_sHeader;
	int Header_size = 14;

	public EthernetLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		m_sHeader = new _ETHERNET_Frame();
	}

	private class _ETHERNET_ADDR {
		private byte[] addr = new byte[6];

		public _ETHERNET_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
			this.addr[4] = (byte) 0x00;
			this.addr[5] = (byte) 0x00;

		}
	}

	private class _ETHERNET_Frame {
		_ETHERNET_ADDR enet_dstaddr;
		_ETHERNET_ADDR enet_srcaddr;
		byte[] enet_type;
		byte[] enet_data;

		public _ETHERNET_Frame() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_data = null;
		}
	}

	public byte[] ObjToByte(_ETHERNET_Frame Header, byte[] input, int length) {// data에
																				// 헤더
																				// 붙여주기
		byte[] buf = new byte[length + Header_size];
		for (int i = 0; i < 6; i++) {
			buf[i] = Header.enet_dstaddr.addr[i];
			buf[i + 6] = Header.enet_srcaddr.addr[i];
		}
		buf[12] = Header.enet_type[0];
		buf[13] = Header.enet_type[1];
		for (int i = 0; i < length; i++)
			buf[14 + i] = input[i];

		return buf;
	}

	public boolean Send(byte[] input, int length) {
		
		m_sHeader.enet_type = intToByte2(0x0800);

		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer(0).Send(bytes, bytes.length);
		return true;
	}

	public boolean arpSend(byte[] input, int length) {
		int opcode = byte2ToInt(input[6], input[7]);
		if (opcode == 1) {
			byte[] b = intToByte2(0xffff);
			byte[] broad = new byte[6];
			for(int i = 0; i < 6; i+=2){
				System.arraycopy(b, 0, broad, i, b.length);
			}
			SetEnetDstAddress(broad);
		}
		else if (opcode == 2) {
			byte[] dst = new byte[6];
			System.arraycopy(input, 18, dst, 0, 6);
			SetEnetDstAddress(dst);
		}
		m_sHeader.enet_type = intToByte2(0x0806);

		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer(0).Send(bytes, bytes.length);
		return true;
	}

	public byte[] RemoveEthernetHeader(byte[] input, int length) {
		byte[] cpyInput = new byte[length - Header_size];
		System.arraycopy(input, Header_size, cpyInput, 0, length - Header_size);
		input = cpyInput;
		return input;
	}

	public synchronized boolean Receive(byte[] input) {
		byte[] data;
		int temp_type = byte2ToInt(input[12], input[13]);
		ARPLayer arpLayer = (ARPLayer) this.GetUpperLayer(0);
		IPLayer ipLayer = (IPLayer) this.GetUpperLayer(1);

		if (temp_type == 0x0800) {
			if (!isMyPacket(input) && (isBroadcast(input) || chkAddr(input))) {
				data = RemoveEthernetHeader(input, input.length);
				ipLayer.Receive(data);
				return true;
			}
		} else if (temp_type == 0x0806) {
			if (!isMyPacket(input) && (isBroadcast(input) || chkAddr(input))) {
				data = RemoveEthernetHeader(input, input.length);
				arpLayer.Receive(data);
				return true;
			}
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
		return (int) (((value1 & 0xff) << 8) | (value2 & 0xff));
	}

	private boolean isBroadcast(byte[] bytes) {
		for (int i = 0; i < 6; i++)
			if (bytes[i] != (byte) 0xff)
				return false;
		return true;
	}

	private boolean isMyPacket(byte[] input) {
		for (int i = 0; i < 6; i++)
			if (m_sHeader.enet_srcaddr.addr[i] != input[6 + i])
				return false;
		return true;
	}

	private boolean chkAddr(byte[] input) {
		byte[] temp = m_sHeader.enet_srcaddr.addr;
		for (int i = 0; i < 6; i++)
			if (m_sHeader.enet_srcaddr.addr[i] != input[i])
				return false;
		return true;
	}

	public void SetEnetSrcAddress(byte[] srcAddress) {
		// TODO Auto-generated method stub
		m_sHeader.enet_srcaddr.addr = srcAddress;
	}

	public void SetEnetDstAddress(byte[] dstAddress) {
		// TODO Auto-generated method stub
		m_sHeader.enet_dstaddr.addr = dstAddress;
	}

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
}
