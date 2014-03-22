package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import core.Console;
import core.Server;
import database.data.AccountData;
import database.data.AnimationData;
import database.data.AreaData;
import database.data.AreaSubData;
import database.data.CharacterData;
import database.data.CollectorData;
import database.data.DropData;
import database.data.ExpData;
import database.data.GuildData;
import database.data.GuildMemberData;
import database.data.HdvData;
import database.data.HouseData;
import database.data.IOTemplateData;
import database.data.ItemData;
import database.data.ItemSetData;
import database.data.ItemTemplateData;
import database.data.JobData;
import database.data.MapData;
import database.data.MonsterData;
import database.data.MountData;
import database.data.MountparkData;
import database.data.NpcAnswerData;
import database.data.NpcData;
import database.data.NpcQuestionData;
import database.data.NpcTemplateData;
import database.data.OtherData;
import database.data.ScriptedCellData;
import database.data.SpellData;
import database.data.TrunkData;

public class Database {
	//connection
	private Connection connection;
	private ReentrantLock locker = new ReentrantLock();
	//data
	private AccountData accountData;
	private AnimationData animationData;
	private AreaData areaData;
	private AreaSubData areaSubData;
	private CharacterData characterData;
	private CollectorData collectorData;
	private GuildData guildData;
	private GuildMemberData guildMemberData;
	private HouseData houseData;
	private ItemData itemData;
	private ItemSetData itemSetData;
	private ItemTemplateData itemTemplateData;
	private JobData jobData;
	private MapData mapData;
	private MonsterData monsterData;
	private MountData mountData;
	private MountparkData mountparkData;
	private NpcAnswerData npcAnswerData;
	private NpcData npcData;
	private NpcQuestionData npcQuestionData;
	private NpcTemplateData npcTemplateData;
	private ScriptedCellData scriptedCellData;
	private SpellData spellData;
	private HdvData hdvData;
	private IOTemplateData ioTemplates;
	private TrunkData trunkData;
	private ExpData expData;
	private OtherData otherData;
	private DropData dropData;
	
	public void initializeData() {
		this.accountData = new AccountData(connection, locker);
		this.animationData = new AnimationData(connection, locker);
		this.areaData = new AreaData(connection, locker);
		this.areaSubData = new AreaSubData(connection, locker);
		this.characterData = new CharacterData(connection, locker);
		this.collectorData = new CollectorData(connection, locker);
		this.guildData = new GuildData(connection, locker);
		this.guildMemberData = new GuildMemberData(connection, locker);
		this.houseData = new HouseData(connection, locker);
		this.itemData = new ItemData(connection, locker);
		this.itemSetData = new ItemSetData(connection, locker);
		this.itemTemplateData = new ItemTemplateData(connection, locker);
		this.jobData = new JobData(connection, locker);
		this.mapData = new MapData(connection, locker);
		this.monsterData = new MonsterData(connection, locker);
		this.npcAnswerData = new NpcAnswerData(connection, locker);
		this.npcData = new NpcData(connection, locker);
		this.npcQuestionData = new NpcQuestionData(connection, locker);
		this.npcTemplateData = new NpcTemplateData(connection, locker);
		this.scriptedCellData = new ScriptedCellData(connection, locker);
		this.spellData = new SpellData(connection, locker);
		this.hdvData = new HdvData(connection, locker);
		this.ioTemplates = new IOTemplateData(connection, locker);
		this.trunkData = new TrunkData(connection, locker);
		this.expData = new ExpData(connection, locker);
		this.otherData = new OtherData(connection, locker);
		this.dropData = new DropData(connection, locker);
		this.mountData = new MountData(connection, locker);
		this.mountparkData = new MountparkData(connection, locker);
	}
	
	public boolean initializeConnection() {
		try {
		  this.connection = DriverManager.getConnection("jdbc:mysql://" +
		  Server.config.getHost() + "/" +
				  Server.config.getDatabaseName(), 
				  Server.config.getUser(), 
				  Server.config.getPass());
		  this.connection.setAutoCommit(true);
		  this.initializeData();
		} catch (SQLException e) {
			Console.instance.writeln("SQL CONNECTION ERROR: "+e.getMessage());
			return false;
		}
		return true;
	}
	
	public void close() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			Console.instance.writeln(" <> Fermeture de la connection échoué: reboot forcé");
		}
	}
	
	public AccountData getAccountData() {
		return accountData;
	}
	public AnimationData getAnimationData() {
		return animationData;
	}
	public AreaData getAreaData() {
		return areaData;
	}
	public AreaSubData getAreaSubData() {
		return areaSubData;
	}
	public CharacterData getCharacterData() {
		return characterData;
	}
	public CollectorData getCollectorData() {
		return collectorData;
	}
	public HouseData getHouseData() {
		return houseData;
	}
	public ItemData getItemData() {
		return itemData;
	}
	public ItemSetData getItemSetData() {
		return itemSetData;
	}
	public ItemTemplateData getItemTemplateData() {
		return itemTemplateData;
	}
	public JobData getJobData() {
		return jobData;
	}
	public MapData getMapData() {
		return mapData;
	}
	public MonsterData getMonsterData() {
		return monsterData;
	}
	public MountData getMountData() {
		return mountData;
	}
	public MountparkData getMountparkData() {
		return mountparkData;
	}
	public NpcAnswerData getNpcAnswerData() {
		return npcAnswerData;
	}
	public NpcData getNpcData() {
		return npcData;
	}
	public NpcQuestionData getNpcQuestionData() {
		return npcQuestionData;
	}
	public NpcTemplateData getNpcTemplateData() {
		return npcTemplateData;
	}
	public ScriptedCellData getScriptedCellData() {
		return scriptedCellData;
	}
	public SpellData getSpellData() {
		return spellData;
	}

	public GuildData getGuildData() {
		return guildData;
	}

	public GuildMemberData getGuildMemberData() {
		return guildMemberData;
	}

	public HdvData getHdvData() {
		return hdvData;
	}

	public void setHdvData(HdvData hdvData) {
		this.hdvData = hdvData;
	}

	public IOTemplateData getIoTemplates() {
		return ioTemplates;
	}

	public void setIoTemplates(IOTemplateData ioTemplates) {
		this.ioTemplates = ioTemplates;
	}

	public TrunkData getTrunkData() {
		return trunkData;
	}

	public void setTrunkData(TrunkData trunkData) {
		this.trunkData = trunkData;
	}

	public ExpData getExpData() {
		return expData;
	}

	public void setExpData(ExpData expData) {
		this.expData = expData;
	}

	public OtherData getOtherData() {
		return otherData;
	}

	public void setOtherData(OtherData otherData) {
		this.otherData = otherData;
	}

	public DropData getDropData() {
		return dropData;
	}

	public void setDropData(DropData dropData) {
		this.dropData = dropData;
	}

	public ReentrantLock getLocker() {
		return locker;
	}
}
