package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ConversationSessionEntity
import com.example.data.model.ConversationTurnEntity
import com.example.data.model.PromptEntity
import com.example.data.model.SessionLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.model.ManagedAccountEntity

@Database(
    entities = [
        PromptEntity::class,
        SessionLogEntity::class,
        ConversationSessionEntity::class,
        ConversationTurnEntity::class,
        ManagedAccountEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun conversationDao(): ConversationDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arena_pilot.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialPrompts(database.promptDao())
                        populateInitialConversations(database.conversationDao())
                        populateInitialAccounts(database.accountDao())
                    }
                }
            }
        }

        suspend fun populateInitialAccounts(dao: AccountDao) {
            val initialAccounts = listOf(
                ManagedAccountEntity(
                    email = "DaynaMorissettehnysz@outlook.com",
                    password = "Ww@123456",
                    label = "Outlook 1 - Dayna",
                    isActive = true
                ),
                ManagedAccountEntity(
                    email = "yeqiangyao846239@outlook.com",
                    password = "Ww@123456",
                    label = "Outlook 2 - yeqiangyao",
                    isActive = false
                ),
                ManagedAccountEntity(
                    email = "leyan402460@outlook.com",
                    password = "Ww@1234563@",
                    label = "Outlook 3 - leyan",
                    isActive = false
                ),
                ManagedAccountEntity(
                    email = "FranciscoVargas89689@outlook.com",
                    password = "Ww@1234563@",
                    label = "Outlook 4 - Francisco",
                    isActive = false
                ),
                ManagedAccountEntity(
                    email = "AmayaKertzmannanj@outlook.com",
                    password = "Ww@1234563@",
                    label = "Outlook 5 - Amaya",
                    isActive = false
                ),
                ManagedAccountEntity(
                    email = "MeganBurgess5352@outlook.com",
                    password = "Ww@1234563@",
                    label = "Outlook 6 - Megan",
                    isActive = false
                )
            )
            dao.insertAll(initialAccounts)
        }

        suspend fun populateInitialPrompts(dao: PromptDao) {
            val defaultPrompts = listOf(
                PromptEntity(
                    title = "交叉辩驳与质疑",
                    content = "请模型A指出模型B回答中的潜在漏洞或事实盲区；同时请模型B指出模型A回答中的薄弱环节，各自给出针对性的反驳论据与反例。",
                    category = "交叉辩驳",
                    orderIndex = 1,
                    isEnabled = true
                ),
                PromptEntity(
                    title = "第一性原理与深度推演",
                    content = "请从第一性原理出发，进一步剖析底层逻辑机制。如果将上下文中的约束条件放大10倍，该方案会如何演化？",
                    category = "深度推演",
                    orderIndex = 2,
                    isEnabled = true
                ),
                PromptEntity(
                    title = "边界测试与极端场景",
                    content = "请给出极端边界输入、异常场景或高并发压测下的具体表现，并编写测试验证样例加以佐证。",
                    category = "边界压测",
                    orderIndex = 3,
                    isEnabled = true
                ),
                PromptEntity(
                    title = "多维对比与权衡矩阵",
                    content = "请梳理上述论点的核心差异，输出一份包含『实现难度、资源开销、扩展性、鲁棒性』的多维度量化对比评分表格。",
                    category = "决策对比",
                    orderIndex = 4,
                    isEnabled = true
                ),
                PromptEntity(
                    title = "实证支撑与理论溯源",
                    content = "针对刚才结论中最关键的一个底层假设，你们有何实证数据、工程经验或权威理论支撑？请补充详细推导。",
                    category = "深度推演",
                    orderIndex = 5,
                    isEnabled = true
                ),
                PromptEntity(
                    title = "终局质询与决策总结",
                    content = "综合上述各轮讨论，请各自用不超过150字的总结性陈词，阐明为什么你的方案在实际工程落地中更具优势？",
                    category = "决策对比",
                    orderIndex = 6,
                    isEnabled = true
                ),
                PromptEntity(
                    title = "未来前沿与架构颠覆",
                    content = "跳出当前的思维定势，如果结合最新的端侧大模型与Agent架构，这项技术未来会有怎样的颠覆性演进？",
                    category = "创意发散",
                    orderIndex = 7,
                    isEnabled = true
                ),
                PromptEntity(
                    title = "高管汇报式通俗化阐述",
                    content = "假设需要向非技术背景的高管或投资人汇报，请用通俗生动的比喻把上述复杂的技术选型与业务价值讲清楚。",
                    category = "通俗化",
                    orderIndex = 8,
                    isEnabled = false
                )
            )
            dao.insertAll(defaultPrompts)
        }

        suspend fun populateInitialConversations(dao: ConversationDao) {
            val now = System.currentTimeMillis()
            val sampleSessionId = dao.insertSession(
                ConversationSessionEntity(
                    title = "分布式缓存双写一致性对比与第一性原理推演",
                    createdAt = now - 3600000,
                    updatedAt = now - 1800000,
                    mode = "ARENA_ONLINE",
                    modelAName = "Model A (Gemini 1.5 Pro)",
                    modelBName = "Model B (Claude 3.5 Sonnet)",
                    totalTurns = 2,
                    winnerVote = "MODEL_A",
                    notes = "模型A在Binlog异步重试与幂等时序分析上更严密，胜出"
                )
            )

            dao.insertTurn(
                ConversationTurnEntity(
                    sessionId = sampleSessionId,
                    turnIndex = 1,
                    userPrompt = "请分析分布式缓存与关系型数据库的双写一致性解决方案",
                    modelAResponse = "主流方案包括延时双删与Binlog异步订阅（Canal/Debezium）。推荐Binlog方案，它通过监听MySQL row格式变动异步失效Redis，天然具备最终一致性与重试补偿。",
                    modelBResponse = "保证一致性须依据CAP权衡。强一致用分布式读写锁（Redisson），最终一致用先写DB再发MQ事务消息。核心原则是只删缓存不更新缓存。",
                    timestamp = now - 3500000,
                    isAutoInjected = false
                )
            )

            dao.insertTurn(
                ConversationTurnEntity(
                    sessionId = sampleSessionId,
                    turnIndex = 2,
                    userPrompt = "请模型A指出模型B回答中的潜在漏洞或事实盲区；同时请模型B指出模型A回答中的薄弱环节，各自给出针对性的反驳论据与反例。",
                    modelAResponse = "【模型A辩驳】：模型B的分布式锁方案在十万QPS高并发下会导致严重的锁竞争与服务雪崩！违背了引入缓存加速的初衷。",
                    modelBResponse = "【模型B辩驳】：模型A的Binlog方案运维复杂度过高，对于中小系统属于过度工程，且缺乏延时波动下的兜底方案。",
                    timestamp = now - 1800000,
                    isAutoInjected = true
                )
            )
        }
    }
}
