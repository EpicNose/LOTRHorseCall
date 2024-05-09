package com.epicnose.lotrcallablehorse.lotr.common;


import lotr.common.LOTRReflection;
import lotr.common.entity.LOTREntities;
import lotr.common.entity.animal.LOTREntityHorse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.UUID;

public class SingleVehicle {
    public int index;
    public String horseName;
    public double horseSpeed;
    public double horseJump;
    public int horseType;
    public int color;
    public double health;
    public int armorid;

    public int variant;

    public boolean isValid;

    public boolean isUsing;
    public UUID entityuuid;
    public World world;
    public UUID ownerUUID;
    public Entity ent;
    public SingleVehicle(){    //登记

    }



    public SingleVehicle(LOTREntityHorse horse){    //登记
        this.horseType=horse.getHorseType();
        this.horseSpeed= horse.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
        this.horseJump=horse.getEntityAttribute(LOTRReflection.getHorseJumpStrength()).getAttributeValue();
        this.health=horse.getEntityAttribute(SharedMonsterAttributes.maxHealth).getAttributeValue();
        this.horseName=horse.getCustomNameTag();
        if(horse.getMountArmor()!=null){
            this.armorid= Item.getIdFromItem(horse.getMountArmor().getItem());
        }

        this.isValid=true;
        this.isUsing=false;
        this.entityuuid=horse.getUniqueID();
        this.world=horse.worldObj;
        this.variant=horse.getHorseVariant();
        horse.setDead();
    }



    public LOTREntityHorse spawnVehicle(EntityPlayer p){   //生成

        LOTREntityHorse newhorse = (LOTREntityHorse) EntityList.createEntityByName(LOTREntities.getStringFromClass(LOTREntityHorse.class), p.worldObj);
//        LOTREntityHorse newhorse=new LOTREntityHorse(world);
        newhorse.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(health);
        newhorse.setHorseType(horseType);
        newhorse.getEntityAttribute(LOTRReflection.getHorseJumpStrength()).setBaseValue(horseJump);
        newhorse.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(horseSpeed);
        newhorse.setCustomNameTag(horseName);
        newhorse.setHorseTamed(true);
        newhorse.setHorseVariant(variant);
        if(armorid!=0){
            if(newhorse.isMountArmorValid(new ItemStack(Item.getItemById(armorid)))){
                newhorse.setMountArmor(new ItemStack(Item.getItemById(armorid)));
            }
        }


        this.isUsing=true;
        this.isValid=true;
        this.ownerUUID=p.getUniqueID();


        newhorse.setHorseSaddled(true);

        newhorse.setLocationAndAngles(p.posX,p.posY,p.posZ,0,0);
        if(!p.worldObj.isRemote){
            p.worldObj.spawnEntityInWorld(newhorse);
        }
//        newhorse.isEntityUndead();
        return newhorse;
    }

    public void writeToNBT(NBTTagCompound nbt){
        if(horseName!=null){
            nbt.setString("HorseName",horseName);
        }

        nbt.setDouble("HorseSpeed",horseSpeed);
        nbt.setDouble("HorseJump",horseJump);
        nbt.setDouble("Health",health);
        nbt.setInteger("ArmorId",armorid);
        nbt.setBoolean("isValid",isValid);
        nbt.setBoolean("isUsing",isUsing);
        nbt.setInteger("index",index);
        nbt.setInteger("HorseType",horseType);
        nbt.setInteger("Variant",variant);
        if(entityuuid!=null){
            nbt.setString("entityuuid",entityuuid.toString());
        }

    }
    public void readFromNBT(NBTTagCompound nbt){
//        SingleVehicle sv=new SingleVehicle();
        if(nbt.hasKey("HorseName")){
            this.horseName=nbt.getString("HorseName");
        }
        if(nbt.hasKey("HorseSpeed")){
            this.horseSpeed=nbt.getDouble("HorseSpeed");

        }
        if(nbt.hasKey("HorseJump")){
            this.horseJump=nbt.getDouble("HorseJump");
        }
        if(nbt.hasKey("Health")){
            this.health=nbt.getDouble("Health");
        }
        if(nbt.hasKey("ArmorId")){
            this.armorid=nbt.getInteger("ArmorId");
        }
        if(nbt.hasKey("isValid")){
            this.isValid=nbt.getBoolean("isValid");
        }
        if(nbt.hasKey("isUsing")){
            this.isUsing=nbt.getBoolean("isUsing");
        }
        if(nbt.hasKey("index")){
            this.index=nbt.getInteger("index");
        }
        if(nbt.hasKey("entityuuid")){
            this.entityuuid=UUID.fromString(nbt.getString("entityuuid"));
        }
        if(nbt.hasKey("HorseType")){
            this.horseType=nbt.getInteger("HorseType");
        }
        if(nbt.hasKey("Variant")){
            this.variant=nbt.getInteger("Variant");
        }

//        MinecraftServer.getServer().getWorldName().equals()

//        return sv;
    }




}
