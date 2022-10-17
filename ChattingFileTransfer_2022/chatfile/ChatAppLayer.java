import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int nUnderLayerCount = 0;
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public ArrayList<BaseLayer> p_aUnderLayer = new ArrayList<BaseLayer>();
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	_CHAT_APP m_sHeader;

	private int Header_size = 4;

	private class _CHAT_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
		byte[] capp_data;

		public _CHAT_APP() {
			this.capp_totlen = new byte[2];
			this.capp_type = 0x00;
			this.capp_unused = 0x00;
			this.capp_data = null;
		}
	}

	public ChatAppLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	private void ResetHeader() {
		m_sHeader = new _CHAT_APP();
	}

	private byte[] objToByte(_CHAT_APP Header, byte[] input, int length) {
		byte[] buf = new byte[length + Header_size];

		buf[0] = Header.capp_totlen[0];
		buf[1] = Header.capp_totlen[1];
		buf[2] = Header.capp_type;
		buf[3] = Header.capp_unused;

		if (length >= 0)
			System.arraycopy(input, 0, buf, Header_size, length);

		return buf;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {
		byte[] cpyInput = new byte[length - Header_size];
		System.arraycopy(input, Header_size, cpyInput, 0, length - Header_size);
		input = cpyInput;
		return input;
	}

	public boolean Send(byte[] input, int length) {
		byte[] bytes;
		m_sHeader.capp_totlen = intToByte2(length);
		bytes = objToByte(m_sHeader, input, input.length);
		TCPLayer tcpLayer = (TCPLayer)this.GetUnderLayer(0);
		tcpLayer.chatSend(bytes, bytes.length);
		return true;
	}

	public synchronized boolean Receive(byte[] input) {
		byte[] data;
		data = RemoveCappHeader(input, input.length);
		this.GetUpperLayer(0).Receive(data);
		return true;
	}

	private byte[] intToByte2(int value) {
		byte[] temp = new byte[2];
		temp[0] |= (byte) ((value & 0xFF00) >> 8);
		temp[1] |= (byte) (value & 0xFF);

		return temp;
	}

	private int byte2ToInt(byte value1, byte value2) {
		return (int) ((value1 << 8) | (value2));
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
