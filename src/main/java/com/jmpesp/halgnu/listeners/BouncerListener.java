package com.jmpesp.halgnu.listeners;

import com.jmpesp.halgnu.managers.ConfigManager;
import com.jmpesp.halgnu.managers.DatabaseManager;
import com.jmpesp.halgnu.models.MemberModel;
import com.jmpesp.halgnu.util.AdminCmdHelper;
import com.jmpesp.halgnu.util.CommandHelper;
import com.jmpesp.halgnu.util.PermissionHelper;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import javax.xml.crypto.Data;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BouncerListener extends ListenerAdapter {

    private String m_inviteCommand = ".invite";
    private String m_whoInvitedCommand = ".whoInvited";
    private String m_memberStatusCommand = ".memberStatus";
    private String m_statusOfMember = ".statusOfMember";
    private String m_enforceCommand = ".enforce";
    private String m_changeStatusCommand = ".changeStatus";
    private String m_removeMemberCommand = ".removeMember";
    
    private List<MemberModel.MemberStatus> neededInvitePermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.OG,
                    MemberModel.MemberStatus.ADMIN,
                    MemberModel.MemberStatus.MEMBER
            ));

    private List<MemberModel.MemberStatus> neededWhoInvitedPermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.OG,
                    MemberModel.MemberStatus.ADMIN,
                    MemberModel.MemberStatus.MEMBER,
                    MemberModel.MemberStatus.PROSPECT
            ));

    private List<MemberModel.MemberStatus> neededMemberStatusPermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.OG,
                    MemberModel.MemberStatus.ADMIN,
                    MemberModel.MemberStatus.MEMBER,
                    MemberModel.MemberStatus.PROSPECT
            ));

    private List<MemberModel.MemberStatus> neededStatusOfMemberPermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.OG,
                    MemberModel.MemberStatus.ADMIN,
                    MemberModel.MemberStatus.MEMBER,
                    MemberModel.MemberStatus.PROSPECT
            ));

    private List<MemberModel.MemberStatus> neededChangeStatusPermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.ADMIN
            ));

    private List<MemberModel.MemberStatus> neededEnforcePermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.ADMIN
            ));

    private List<MemberModel.MemberStatus> neededRemoveMemberPermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.ADMIN
            ));
    
    private boolean m_enforce = false;

    public static void sendHelpMsg(GenericMessageEvent event) {
        event.getBot().sendIRC().message(event.getUser().getNick(), ".invite <user> - Used to invite user to room");
        event.getBot().sendIRC().message(event.getUser().getNick(), ".whoInvited <user> - Returns who invited the user");
        event.getBot().sendIRC().message(event.getUser().getNick(),".memberStatus - Returns your member status");
        event.getBot().sendIRC().message(event.getUser().getNick(), ".statusOfMember <member> - Returns status of desired member");
        event.getBot().sendIRC().message(event.getUser().getNick(), ".enforce - Enables/Disables bouncer enforcement");
        event.getBot().sendIRC().message(event.getUser().getNick(), ".changeStatus <user> <status> - Change users membership status");
        event.getBot().sendIRC().message(event.getUser().getNick(), ".removeMember <user> - Removes user from the room");
    }
    
    @Override
    public void onJoin(final JoinEvent join) throws Exception {

        if(m_enforce) {
            if (!(join.getUser().getNick().trim().equals(ConfigManager.getInstance().getIrcNick()))) {

                MemberModel member = DatabaseManager.getInstance().getMemberByUsername(join.getUser().getNick().trim());

                if (member != null) {
                    join.respond("Authorized");
                } else {
                    AdminCmdHelper.kickUserFromRoom(join.getUser().getNick(), "Not-Authorized");
                }
            }
        }
    }

    @Override
    public void onGenericMessage(final GenericMessageEvent event) throws Exception {

        // Handle enforce command
        if (event.getMessage().startsWith(m_enforceCommand)) {
            handleEnforceCommand(event);
        }

        // Handle Invite Command
        if (event.getMessage().startsWith(m_inviteCommand)) {
            handleInviteCommand(event);
        }

        // Handle WhoIvited Command
        if (event.getMessage().startsWith(m_whoInvitedCommand)) {
            handleWhoInvitedCommand(event);
        }

        // Handle MemberStatus Command
        if (event.getMessage().startsWith(m_memberStatusCommand)) {
            handleMemberStatusCommand(event);
        }

        // Handle statusOfMember Command
        if (event.getMessage().startsWith(m_statusOfMember)) {
            handleStatusOfMemberCommand(event);
        }
        
        // Handle changeStatus Command
        if (event.getMessage().startsWith(m_changeStatusCommand)) {
            handleChangeStatusCommand(event);
        }
        
        // Handle remove member
        if (event.getMessage().startsWith(m_removeMemberCommand)) {
            handleRemoveMemberCommand(event);
        }
    }
    
    private void handleRemoveMemberCommand(GenericMessageEvent event) {
        if(PermissionHelper.HasPermissionFromList(neededRemoveMemberPermissions, event.getUser().getNick())) {
            if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 1)) {
                try {
                    MemberModel member = DatabaseManager.getInstance().getMemberByUsername(CommandHelper.removeCommandFromString(event.getMessage()).trim());
                    if (member != null) {
                        DatabaseManager.getInstance().getMemberDao().delete(member);
                        event.respond("User removed from database");
                        AdminCmdHelper.kickUserFromRoom(CommandHelper.removeCommandFromString(event.getMessage()).trim(), "Membership_Revoked");
                        
                    } else {
                        event.respond("User does not exist");
                    }
                } catch (SQLException e) {
                    event.respond("User does not exist");
                }
            } else {
                event.respond("Ex: " + m_removeMemberCommand + " <user>");
            }
        } else {
            event.respond("Permission denied");
        }
    }
    
    private void handleChangeStatusCommand(GenericMessageEvent event) {
        if(PermissionHelper.HasPermissionFromList(neededChangeStatusPermissions, event.getUser().getNick())) {
            if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 2)) {
                String arguments = CommandHelper.removeCommandFromString(event.getMessage());
                String[] splitArguments = arguments.split(" ");
                
                if(splitArguments.length == 2 ) {
                    try {
                        MemberModel member = DatabaseManager.getInstance().getMemberByUsername(splitArguments[0]);
                        
                        if(member != null) {
                            String status = splitArguments[1];
                            boolean success = false;
                            
                            if(status.equals("og")) {
                                success = true;
                                member.setMemberStatus(MemberModel.MemberStatus.OG);
                                DatabaseManager.getInstance().getMemberDao().update(member);
                            } else if (status.equals("admin")) {
                                success = true;
                                member.setMemberStatus(MemberModel.MemberStatus.ADMIN);
                                DatabaseManager.getInstance().getMemberDao().update(member);
                            } else if (status.equals("member")) {
                                success = true;
                                member.setMemberStatus(MemberModel.MemberStatus.MEMBER);
                                DatabaseManager.getInstance().getMemberDao().update(member);
                            } else if (status.equals("prospect")) {
                                success = true;
                                member.setMemberStatus(MemberModel.MemberStatus.PROSPECT);
                                DatabaseManager.getInstance().getMemberDao().update(member);
                            } else {
                                event.respond("Unknown status detected");
                            }
                            
                            if(success) {
                                event.respond("Status update completed");
                            }
                            
                        } else {
                            event.respond("Member not found");
                        }
                        
                    } catch (SQLException e) {
                        e.printStackTrace();
                        event.respond("Issue with query");
                    }

                } else {
                    event.respond("Ex. <username> <status> | admin,og,member,prospect 2");
                }
            } else {
                event.respond("Ex. <username> <status> | admin,og,member,prospect 1");
            }
        }
        else {
            event.respond("Permission denied");
        }
    }

    private void handleEnforceCommand(GenericMessageEvent event) {
        int numKicked = 0;

        if(PermissionHelper.HasPermissionFromList(neededEnforcePermissions, event.getUser().getNick())) {
            if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 0)) {

                m_enforce = !m_enforce;

                if(m_enforce) {
                    event.respond("Activating enforcement mode");

                    event.respond("Scanning room");

                    for (User user : event.getBot().getUserBot().getChannels().first().getUsers()) {
                        if(!(user.getNick().trim().equals(ConfigManager.getInstance().getIrcNick())))
                        try {
                            MemberModel member = DatabaseManager.getInstance().getMemberByUsername(user.getNick().trim());
                            if (member == null) {
                                AdminCmdHelper.kickUserFromRoom(user.getNick(), "Not-Authorized");
                                numKicked += 1;
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if(numKicked == 1) {
                        event.respond("Removed " + numKicked + " user");
                    } else {
                        event.respond("Removed " + numKicked + " users");
                    }

                    event.respond("Scan complete");

                } else {
                    event.respond("Deactivating enforcement mode");
                }
            }
        } else {
            event.respond("Permission denied");
        }
    }

    private void handleInviteCommand(GenericMessageEvent event) {
        if(PermissionHelper.HasPermissionFromList(neededInvitePermissions, event.getUser().getNick())) {
            if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 1)) {
                if(DatabaseManager.getInstance().createMember(CommandHelper.removeCommandFromString(event.getMessage()).trim()
                        ,event.getUser().getNick())) {
                    event.respond("User added to registry");
                } else {
                    event.respond("User already in registry");
                }
            } else {
                event.respond("Ex: " + m_inviteCommand + " <usernamehere>");
            }
        } else {
            event.respond("Permission denied");
        }
    }

    private void handleWhoInvitedCommand(GenericMessageEvent event) {
        if(PermissionHelper.HasPermissionFromList(neededWhoInvitedPermissions, event.getUser().getNick())) {

            if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 1)) {
                MemberModel member = null;
                try {
                    member = DatabaseManager.getInstance()
                            .getMemberByUsername(CommandHelper.removeCommandFromString(event.getMessage()).trim());

                    if(member != null) {
                        event.respond(member.getInvitedBy() + " invited " + member.getUserName() + " on " + member.getDateInvited());
                    } else {
                        event.respond("User not in registry");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.respond("User not in registry");
                }
            } else {
                event.respond("Ex: " + m_whoInvitedCommand + " <usernamehere>");
            }
        } else {
            event.respond("Permission denied");
        }
    }

    private void handleMemberStatusCommand(GenericMessageEvent event) {
        if(PermissionHelper.HasPermissionFromList(neededMemberStatusPermissions, event.getUser().getNick())) {

            if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 0)) {
                MemberModel member = null;
                try {
                    member = DatabaseManager.getInstance()
                            .getMemberByUsername(event.getUser().getNick().trim());

                    if(member != null) {
                        if(member.getMemberStatus().equals(MemberModel.MemberStatus.OG)) {
                            event.respond("Your member status is: OG");
                        }

                        if(member.getMemberStatus().equals(MemberModel.MemberStatus.ADMIN)) {
                            event.respond("Your member status is: ADMIN");
                        }

                        if(member.getMemberStatus().equals(MemberModel.MemberStatus.MEMBER)) {
                            event.respond("Your member status is: MEMBER");
                        }

                        if(member.getMemberStatus().equals(MemberModel.MemberStatus.PROSPECT)) {
                            event.respond("Your member status is: PROSPECT");
                        }
                    } else {
                        event.respond("User not in registry");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.respond("User not in registry");
                }
            } else {
                event.respond("Ex: " + m_whoInvitedCommand + " <usernamehere>");
            }
        } else {
            event.respond("Permission denied");
        }
    }

    private void handleStatusOfMemberCommand(GenericMessageEvent event) {
        // Handle statusOfMember Command
        if (event.getMessage().startsWith(m_statusOfMember)) {
            if(PermissionHelper.HasPermissionFromList(neededStatusOfMemberPermissions, event.getUser().getNick())) {

                if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 1)) {
                    MemberModel member = null;
                    try {
                        member = DatabaseManager.getInstance()
                                .getMemberByUsername(CommandHelper.removeCommandFromString(event.getMessage()).trim());

                        if(member != null) {
                            if(member.getMemberStatus().equals(MemberModel.MemberStatus.OG)) {
                                event.respond(member.getUserName()+"'s member status is: OG");
                            }

                            if(member.getMemberStatus().equals(MemberModel.MemberStatus.ADMIN)) {
                                event.respond(member.getUserName()+"'s member status is: ADMIN");
                            }

                            if(member.getMemberStatus().equals(MemberModel.MemberStatus.MEMBER)) {
                                event.respond(member.getUserName()+"'s member status is: MEMBER");
                            }

                            if(member.getMemberStatus().equals(MemberModel.MemberStatus.PROSPECT)) {
                                event.respond(member.getUserName()+"'s member status is: PROSPECT");
                            }
                        } else {
                            event.respond("User not in registry");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        event.respond("User not in registry");
                    }
                } else {
                    event.respond("Ex: " + m_whoInvitedCommand + " <usernamehere>");
                }
            } else {
                event.respond("Permission denied");
            }
        }
    }
}
